package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.entity.paymentGateway.StripeCredentials;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.repository.payment.StripeCredRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.wallet.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
public class StripeWebhookService {

    @Autowired
    private StripeCredRepository stripeCredRepository;

    @Autowired
    private WalletDepositRepository depositRepository;

    @Autowired
    private WalletService walletService;
    @Autowired
    private TransactionRepository transactionRepo;

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        try {
            // Get Stripe credentials
            Optional<StripeCredentials> stripeCredOpt = stripeCredRepository.findById(1L);
            if (stripeCredOpt.isEmpty()) {
                throw ServiceExceptions.notFound("Stripe credentials not configured");
            }

            StripeCredentials stripeCreds = stripeCredOpt.get();
            Stripe.apiKey = stripeCreds.getSecretKey();

            // Verify webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, stripeCreds.getWebhookSecret());

            // Route to appropriate handler based on event type
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                case "checkout.session.expired":
                    handleCheckoutSessionExpired(event);
                    break;

                case "payment_intent.created":
                    handlePaymentIntentCreated(event);
                    break;
                default:
                    // Log unhandled event types for debugging
                    System.out.println("Unhandled event type: " + event.getType());
                    break;
            }
        } catch (Exception e) {
            throw ServiceExceptions.notFound("Webhook processing failed: " + e.getMessage());
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Convert the full Event object back to JSON string to manually parse
            String eventJson = event.toJson();
            JsonNode rootNode = mapper.readTree(eventJson);

            JsonNode sessionNode = rootNode.path("data").path("object");

            if (!sessionNode.isMissingNode()) {
                String paymentStatus = sessionNode.path("payment_status").asText();
                String sessionId = sessionNode.path("id").asText();
                String depositReference = sessionNode.path("metadata").path("deposit_reference").asText();

                if ("paid".equals(paymentStatus) && depositReference != null && !depositReference.isEmpty()) {
                    processSuccessfulPaymentInternal(depositReference, sessionId);
                } else {
                    System.out.println("Payment status not paid or missing deposit reference");
                }
            } else {
                System.out.println("Missing session object in webhook data");
            }
        } catch (Exception e) {
            throw ServiceExceptions.notFound("Failed to manually parse checkout session completion: " + e.getMessage(), e);
        }
    }


    private void handlePaymentIntentSucceeded(Event event) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Convert the full Event object back to JSON string to manually parse
            String eventJson = event.toJson();
            JsonNode rootNode = mapper.readTree(eventJson);

            JsonNode sessionNode = rootNode.path("data").path("object");

            if (!sessionNode.isMissingNode()) {
                String paymentStatus = sessionNode.path("status").asText();
                String sessionId = sessionNode.path("id").asText();
                String depositReference = sessionNode.path("metadata").path("deposit_reference").asText();

                if ("succeeded".equals(paymentStatus) && depositReference != null && !depositReference.isEmpty()) {
                    processSuccessfulPaymentInternal(depositReference, sessionId);
                } else {
                    System.out.println("Payment status not paid or missing deposit reference");
                }
            } else {
                System.out.println("Missing session object in webhook data");
            }
        } catch (Exception e) {
            throw ServiceExceptions.notFound("Failed to handle payment intent success: " + e.getMessage());
        }
    }

    private void handlePaymentIntentFailed(Event event) {
        // Handle failed payments - could update deposit status to failed
        System.out.println("Payment intent failed: " + event.getId());
    }

    private void handleCheckoutSessionExpired(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);

            if (session != null) {
                String depositReference = session.getMetadata().get("deposit_reference");

                if (depositReference != null) {
                    markDepositAsExpiredInternal(depositReference);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to handle expired session: " + e.getMessage());
        }
    }

    private void handlePaymentIntentCreated(Event event) {
        try {
            // First, let's try to get the raw data object
            Optional<com.stripe.model.StripeObject> dataObjectOpt = event.getDataObjectDeserializer().getObject();

            if (dataObjectOpt.isEmpty()) {
                System.out.println("No data object found in payment_intent.created event: " + event.getId());
                return;
            }

            com.stripe.model.StripeObject dataObject = dataObjectOpt.get();

            // Check if it's actually a PaymentIntent
            if (dataObject instanceof PaymentIntent paymentIntent) {
                System.out.println("Payment intent created: " + paymentIntent.getId() +
                        ", Amount: " + (paymentIntent.getAmount() / 100.0) +
                        ", Currency: " + paymentIntent.getCurrency() +
                        ", Status: " + paymentIntent.getStatus());

                // Check if metadata exists
                if (paymentIntent.getMetadata() == null || paymentIntent.getMetadata().isEmpty()) {
                    System.out.println("Payment intent " + paymentIntent.getId() + " has no metadata - likely created by checkout session");
                    return;
                }

                String depositReference = paymentIntent.getMetadata().get("deposit_reference");

                if (depositReference != null) {
                    Optional<WalletDeposit> depositOpt = depositRepository.findByReference(depositReference);

                    if (depositOpt.isPresent()) {
                        WalletDeposit deposit = depositOpt.get();

                        // Update deposit with payment intent details if still pending
                        if (deposit.getStatus() == DepositStatus.PENDING) {
                            String currentRemarks = deposit.getRemarks() != null ? deposit.getRemarks() : "";
                            deposit.setRemarks(currentRemarks + " - Payment Intent: " + paymentIntent.getId());
                            depositRepository.save(deposit);

                            System.out.println("Updated deposit " + depositReference + " with payment intent: " + paymentIntent.getId());
                        }
                    } else {
                        System.out.println("Deposit not found for payment intent: " + paymentIntent.getId() +
                                ", Reference: " + depositReference);
                    }
                } else {
                    System.out.println("Payment intent created without deposit reference: " + paymentIntent.getId() +
                            " - this is normal for checkout session payments");
                }
            } else {
                System.out.println("Data object is not a PaymentIntent: " + dataObject.getClass().getSimpleName() +
                        " for event: " + event.getId());
            }

        } catch (Exception e) {
            System.out.println("Failed to handle payment intent creation for event: " + event.getId() +
                    ", Error: " + e.getMessage());
            // Don't throw exception to avoid disrupting other webhook processing
        }
    }

    protected void processSuccessfulPaymentInternal(String depositReference, String sessionId) {
        Optional<WalletDeposit> depositOpt = depositRepository.findByReference(depositReference);

        if (depositOpt.isPresent()) {
            WalletDeposit deposit = depositOpt.get();

            // Only process if still pending
            if (deposit.getStatus() == DepositStatus.PENDING) {
                // Update deposit status to approved
                deposit.setStatus(DepositStatus.APPROVED);
                deposit.setApprovedAt(UserDateTimeUtil.now());
                deposit.setApprovedBy(1L); // System approval
                deposit.setRemarks(deposit.getRemarks() + " - Stripe Session: " + sessionId);

                depositRepository.save(deposit);
                System.out.println("For User ID: " + deposit.getUserId() + ", Deposit Amount: " + deposit.getAmount());
                // Add amount to user's wallet balance
                walletService.addToWalletBalance(deposit.getUserId(), deposit.getAmount());

                Transaction txn = Transaction.builder()
                        .type("DEPOSIT")
                        .amount(deposit.getAmount())
                        .currency("USD")
                        .convertedAmount(deposit.getExchangedAmount() != null ? String.valueOf(deposit.getExchangedAmount()) : null)
                        .exchangeRate(deposit.getExchangeRate())
                        .description("Deposit - Ref: " + deposit.getReference())
                        .userId(deposit.getUserId())
                        .createdBy(String.valueOf(deposit.getUserId()))
                        .createdAt(UserDateTimeUtil.now())
                        .sourceType(TransactionSourceType.DEPOSIT.name())
                        .sourceId(deposit.getId())
                        .active(true)
                        .reference(deposit.getReference())
                        .build();

                transactionRepo.save(txn);


                System.out.println("Successfully processed payment for deposit: " + depositReference);
            }
        } else {
            System.out.println("Deposit not found for reference: " + depositReference);
        }
    }

    protected void markDepositAsExpiredInternal(String depositReference) {
        Optional<WalletDeposit> depositOpt = depositRepository.findByReference(depositReference);

        if (depositOpt.isPresent()) {
            WalletDeposit deposit = depositOpt.get();

            // Only mark as rejected if still pending
            if (deposit.getStatus() == DepositStatus.PENDING) {
                deposit.setStatus(DepositStatus.REJECTED);
                deposit.setApprovedAt(UserDateTimeUtil.now());
                deposit.setApprovedBy(1L); // System
                deposit.setRemarks(deposit.getRemarks() + " - Expired: Checkout session expired");

                depositRepository.save(deposit);

                System.out.println("Marked deposit as expired: " + depositReference);
            }
        }
    }
}
