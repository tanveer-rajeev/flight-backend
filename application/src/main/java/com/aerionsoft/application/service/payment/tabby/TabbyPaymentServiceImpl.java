package com.aerionsoft.application.service.payment.tabby;

import com.aerionsoft.application.config.TabbyProperties;
import com.aerionsoft.application.dto.payment.tabby.*;
import com.aerionsoft.application.exception.TabbyInvalidPaymentStateException;
import com.aerionsoft.application.service.payment.TabbyPaymentStatusUpdater;
import com.aerionsoft.application.dto.payment.PaymentCompletionRequest;
import com.aerionsoft.application.entity.paymentGateway.TabbyPayment;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.payment.PaymentStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.enums.wallet.PaymentProvider;
import com.aerionsoft.application.exception.TabbyCheckoutRejectedException;
import com.aerionsoft.application.exception.TabbyPaymentNotFoundException;
import com.aerionsoft.application.exception.TabbyResponseException;
import com.aerionsoft.application.repository.payment.TabbyPaymentRepository;
import com.aerionsoft.application.service.payment.PaymentCompletionService;
import com.aerionsoft.application.util.PaymentCallBackUrlBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class TabbyPaymentServiceImpl implements TabbyPaymentService {

    private final TabbyApiClient tabbyApiClient;
    private final TabbyProperties tabbyProperties;
    private final TabbyPaymentRepository tabbyPaymentRepository;
    private final TabbyPaymentStatusUpdater statusUpdater;
    private final PaymentCompletionService paymentCompletionService;

    @Override
    public TabbyCheckoutResult initiateCheckout(TabbyCheckoutCommand command) {

        log.info("Initiating Tabby checkout for orderId={}", command.referenceId());

        Optional<TabbyPayment> existing = tabbyPaymentRepository.findByOrderId(command.referenceId());
        if (existing.isPresent()) {
            TabbyPayment payment = existing.get();
            log.info("Checkout already exists for orderId={}, returning existing session tabbyPaymentId={} status={}",
                    command.referenceId(), payment.getPaymentId(), payment.getStatus());
            return new TabbyCheckoutResult(payment.getPaymentId(), payment.getStatus(),
                    payment.getCheckoutUrl());
        }

        TabbyCheckoutSessionRequest tabbyRequest = toTabbyRequest(command);

        TabbyCheckoutSessionResponse response = tabbyApiClient.createCheckoutSession(tabbyRequest);

        log.info("Tabby checkout session created for orderId={}, tabbyPaymentId={}, status={}",
                command.referenceId(), response.payment().id(), response.status());

        if (PaymentStatus.REJECTED.toString().equalsIgnoreCase(response.status())) {
            String rejectionReason = null;
            if (response.configuration() != null && response.configuration().products() != null) {
                rejectionReason = response.configuration().products().values().stream()
                        .map(TabbyCheckoutSessionResponse.ProductStatus::rejectionReason)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }
            log.warn("Tabby rejected checkout for orderId={}, reason={}", command.referenceId(), rejectionReason);
            throw new TabbyCheckoutRejectedException(command.referenceId(), rejectionReason);
        }

        String redirectUrl = response.configuration().availableProducts().values().stream()
                .flatMap(List::stream)
                .findFirst()
                .map(TabbyCheckoutSessionResponse.AvailableProduct::webUrl)
                .orElseThrow(() -> {
                    log.error("Tabby response missing redirect URL for orderId={}, status={}",
                            command.referenceId(), response.status());
                    return new TabbyResponseException(
                            command.referenceId(), response.status(), "no redirect URL in response");
                });

        TabbyPayment saved = statusUpdater.saveNewPayment(TabbyPayment.builder()
                .userId(command.userId())
                .orderId(command.referenceId())
                .paymentId(response.payment().id())
                .amount(command.amount())
                .currency(Currency.valueOf(command.currency()))
                .status(PaymentStatus.CREATED)
                .checkoutUrl(redirectUrl)
                .build());

        log.info("Persisted new TabbyPayment for orderId={}, tabbyPaymentId={}, status=CREATED",
                command.referenceId(), saved.getPaymentId());

        return new TabbyCheckoutResult(saved.getPaymentId(), PaymentStatus.CREATED, redirectUrl);
    }

    @Override
    public void handleWebhook(TabbyWebhookPayload payload) {

        if (payload.status().equals(PaymentStatus.CLOSED.name())) {
            log.warn("Received webhook for same status for tabbyPaymentId={}", payload.id());
            return;
        }

        log.info("Handling webhook for tabbyPaymentId={}, reportedStatus={}", payload.id(), payload.status());

        Optional<TabbyPayment> maybeEntity = tabbyPaymentRepository.findByPaymentId(payload.id());

        if (maybeEntity.isEmpty()) {
            log.warn("Received webhook for unknown tabbyPaymentId={} - no local record exists", payload.id());
            return;
        }

        TabbyPayment entity = maybeEntity.get();

        if (isTerminal(entity.getStatus())) {
            log.debug("Ignoring webhook for tabbyPaymentId={} - already terminal at status={}",
                    payload.id(), entity.getStatus());
            return;
        }

        PaymentStatus incomingStatus = mapStatus(payload.status());
        TabbyPaymentDetailsResponse confirmed = tabbyApiClient.getPayment(payload.id());

        if (confirmed.status() != incomingStatus) {
            log.warn("Webhook status mismatch for tabbyPaymentId={} - webhook said {}, Retrieve API said {}. " +
                            "Using Retrieve API value.",
                    payload.id(), incomingStatus, confirmed.status());
        }

        Optional<TabbyPayment> updated = statusUpdater.applyStatusIfNotTerminal(payload.id(), confirmed.status());

        if (updated.isPresent()) {
            log.info("Webhook applied status={} for tabbyPaymentId={}", confirmed.status(), payload.id());
            reactToStatus(updated.get(), confirmed.status());
        } else {
            log.debug("Webhook lost the race for tabbyPaymentId={} - already finalized elsewhere", payload.id());
        }
    }

    @Override
    public TabbyPayment confirmPayment(String tabbyPaymentId) {

        log.info("Confirming payment for tabbyPaymentId={}", tabbyPaymentId);

        TabbyPayment existing = tabbyPaymentRepository.findByPaymentId(tabbyPaymentId)
                .orElseThrow(() -> {
                    log.warn("Confirm requested for unknown tabbyPaymentId={}", tabbyPaymentId);
                    return new TabbyPaymentNotFoundException(tabbyPaymentId);
                });

        if (isTerminal(existing.getStatus())) {
            log.debug("Skipping confirm for tabbyPaymentId={} - already terminal at status={}",
                    tabbyPaymentId, existing.getStatus());
            return existing;
        }

        TabbyPaymentDetailsResponse tabbyPayment = tabbyApiClient.getPayment(tabbyPaymentId);

        log.info("Tabby reports status={} for tabbyPaymentId={}", tabbyPayment.status(), tabbyPaymentId);

        Optional<TabbyPayment> updated =
                statusUpdater.applyStatusIfNotTerminal(tabbyPaymentId, tabbyPayment.status());

        if (updated.isPresent()) {
            log.info("Confirm applied status={} for tabbyPaymentId={}", tabbyPayment.status(), tabbyPaymentId);
            reactToStatus(updated.get(), tabbyPayment.status());
        } else {
            log.debug("Confirm lost the race for tabbyPaymentId={} - already finalized elsewhere", tabbyPaymentId);
        }

        return updated.orElse(existing);
    }

    @Override
    @Transactional
    public TabbyPayment capturePayment(String tabbyPaymentId, BigDecimal amount) {

        TabbyPayment entity = tabbyPaymentRepository.findByPaymentId(tabbyPaymentId)
                .orElseThrow(() -> new TabbyPaymentNotFoundException(tabbyPaymentId));

        if (entity.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new TabbyInvalidPaymentStateException(
                    tabbyPaymentId, entity.getStatus(), PaymentStatus.AUTHORIZED);
        }

        BigDecimal captureAmount = amount != null ? amount : entity.getAmount();

        TabbyPaymentDetailsResponse response =
                tabbyApiClient.capturePayment(tabbyPaymentId, new CapturePaymentRequest(captureAmount));

        log.info("Get capture api response with status {} for amount {}", response.status(), response.amount());

        TabbyPaymentDetailsResponse confirmed = tabbyApiClient.getPayment(tabbyPaymentId);

        reactToStatus(entity, confirmed.status());
        entity.setAmount(captureAmount);

        return tabbyPaymentRepository.save(entity);
    }

    private void reactToStatus(TabbyPayment entity, PaymentStatus status) {
        switch (status) {
            case CLOSED -> onClosed(entity);
            case REJECTED -> onRejected(entity);
            case EXPIRED -> onExpired(entity);
            case AUTHORIZED -> log.debug("Payment {} authorized, awaiting Tabby auto-capture", entity.getPaymentId());
            default -> log.warn("Unhandled Tabby status {} for payment {}", status, entity.getPaymentId());
        }
    }

    private void onClosed(TabbyPayment entity) {
        log.info("Payment closed - orderId={}, tabbyPaymentId={}, amount={}",
                entity.getOrderId(), entity.getPaymentId(), entity.getAmount());

        paymentCompletionService.completeDeposit(
                PaymentCompletionRequest.builder()
                        .userId(entity.getUserId())
                        .amount(entity.getAmount().doubleValue())
                        .currency(entity.getCurrency())
                        .depositType(DepositType.INSTANT)
                        .paymentProvider(PaymentProvider.TABBY.name())
                        .paymentTransactionId(entity.getPaymentId())
                        .reference(entity.getOrderId())
                        .description("Tabby wallet deposit")
                        .createdBy(entity.getUserId())
                        .build()
        );
    }

    private boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.CLOSED
                || status == PaymentStatus.REJECTED
                || status == PaymentStatus.EXPIRED;
    }

    private void onRejected(TabbyPayment entity) {
        log.info("Payment rejected - orderId={}, tabbyPaymentId={}", entity.getOrderId(), entity.getPaymentId());
    }

    private void onExpired(TabbyPayment entity) {
        log.info("Payment expired - orderId={}, tabbyPaymentId={}", entity.getOrderId(), entity.getPaymentId());
    }

    @Override
    public Optional<TabbyPayment> findByReferenceId(String referenceId) {
        return tabbyPaymentRepository.findByOrderId(referenceId);
    }

    @Override
    public Optional<TabbyPayment> findByPaymentId(String paymentId) {
        return tabbyPaymentRepository.findByPaymentId(paymentId);
    }

    @Override
    public List<TabbyPayment> findAll() {
        return tabbyPaymentRepository.findAll();
    }

    private PaymentStatus mapStatus(String rawStatus) {
        try {
            return PaymentStatus.valueOf(rawStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unrecognized Tabby status '{}', defaulting to UNKNOWN", rawStatus);
            return PaymentStatus.UNKNOWN;
        }
    }

    private TabbyCheckoutSessionRequest toTabbyRequest(TabbyCheckoutCommand command) {
        var buyer = new TabbyCheckoutSessionRequest.Buyer(command.buyerPhone(), command.buyerEmail());

        var order = new TabbyCheckoutSessionRequest.Order(command.referenceId());
        var payment = new TabbyCheckoutSessionRequest.Payment(
                command.amount(), command.currency(), buyer, order, command.referenceId());
        return new TabbyCheckoutSessionRequest(payment, "en", tabbyProperties.merchantCode(), buildMerchantUrls());
    }

    private TabbyCheckoutSessionRequest.MerchantUrls buildMerchantUrls() {

        return new TabbyCheckoutSessionRequest.MerchantUrls(
                PaymentCallBackUrlBuilder.build(tabbyProperties.merchant_urls().success(), PaymentStatus.SUCCESS,
                        PaymentProvider.TABBY),
                PaymentCallBackUrlBuilder.build(tabbyProperties.merchant_urls().cancel(),PaymentStatus.CANCELLED,
                        PaymentProvider.TABBY),
                PaymentCallBackUrlBuilder.build(tabbyProperties.merchant_urls().failure(),PaymentStatus.FAILED,
                        PaymentProvider.TABBY)

        );
    }
}