package com.aerionsoft.application.service.payment;

import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.enums.wallet.PaymentProvider;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.service.wallet.ReferenceGeneratorService;
import com.aerionsoft.application.service.wallet.WalletService;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.payment.SslCommerzCallbackRequest;
import com.aerionsoft.application.dto.payment.SslCommerzInitRequest;
import com.aerionsoft.application.dto.payment.SslCommerzInitResponse;
import com.aerionsoft.application.entity.paymentGateway.SslCommerzPayment;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.repository.payment.SslCommerzPaymentRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SslCommerzService {

    private final SslCommerzPaymentRepository sslCommerzPaymentRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final ReferenceGeneratorService referenceGeneratorService;

    @Value("${ssl.commerce.store.id:}")
    private String storeId;

    @Value("${ssl.commerce.store.password:}")
    private String storePassword;

    @Value("${ssl.commerce.api.url:https://sandbox.sslcommerz.com}")
    private String apiBaseUrl;

    @Value("${ssl.commerce.success.url:}")
    private String successUrl;

    @Value("${ssl.commerce.fail.url:}")
    private String failUrl;

    @Value("${ssl.commerce.cancel.url:}")
    private String cancelUrl;

    @Value("${ssl.commerce.callback.url:}")
    private String callbackUrl;

    @Autowired
    private CurrencyService currencyService;

    //Initialize payment with SSLCommerz
    @Transactional
    public SslCommerzInitResponse initPayment(SslCommerzInitRequest request, Long userId) {
        try {
            // Generate unique transaction ID
            String tranId = "TT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

            // Create payment record
            SslCommerzPayment payment = SslCommerzPayment.builder()
                    .bookingId(request.getBookingId())
                    .userId(userId)
                    .method(PaymentProvider.SSL_COMMERZ)
                    .tranId(tranId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status("INITIATED")
                    .customerName(request.getCustomerName())
                    .customerEmail(request.getCustomerEmail())
                    .customerPhone(request.getCustomerPhone())
                    .createdAt(UserDateTimeUtil.now())
                    .updatedAt(UserDateTimeUtil.now())
                    .build();

            sslCommerzPaymentRepository.save(payment);

            // Prepare SSLCommerz request
            Map<String, String> requestData = new HashMap<>();
            requestData.put("store_id", storeId);
            requestData.put("store_passwd", storePassword);
            requestData.put("total_amount", request.getAmount().toString());
            requestData.put("currency", request.getCurrency());
            requestData.put("tran_id", tranId);
            requestData.put("success_url", successUrl);
            requestData.put("fail_url", failUrl);
            requestData.put("cancel_url", cancelUrl);
            requestData.put("ipn_url", callbackUrl);
            requestData.put("emi_option", "0");
            requestData.put("cus_name", request.getCustomerName());
            requestData.put("cus_email", request.getCustomerEmail());
            requestData.put("cus_phone", request.getCustomerPhone());
            requestData.put("cus_add1", "Dhaka");
            requestData.put("cus_city", "Dhaka");
            requestData.put("cus_country", "Bangladesh");
            requestData.put("shipping_method", "NO");
            requestData.put("num_of_item", "1");
            requestData.put("product_name", "Booking Payment");
            requestData.put("product_category", "Travel");
            requestData.put("product_profile", "general");

            // Call SSLCommerz API
            String apiUrl = apiBaseUrl + "/gwprocess/v4/api.php";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            requestData.forEach(formData::add);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            log.info("SSLCommerz API Response: {}", response.getBody());

            // Parse response
            if (response.getBody() != null && response.getBody().contains("\"status\":\"SUCCESS\"")) {
                String redirectUrl = extractRedirectUrl(response.getBody());
                String sessionKey = extractSessionKey(response.getBody());

                // Update payment with session key
                payment.setSessionKey(sessionKey);
                payment.setRawResponse(response.getBody());
                sslCommerzPaymentRepository.save(payment);

                return SslCommerzInitResponse.builder()
                        .tranId(tranId)
                        .sessionKey(sessionKey)
                        .redirectUrl(redirectUrl)
                        .status("INITIATED")
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .bookingId(request.getBookingId())
                        .build();
            } else {
                throw ServiceExceptions.payment("SSLCommerz API call failed: " + response.getBody());
            }

        } catch (Exception e) {
            log.error("Error initializing SSLCommerz payment: {}", e.getMessage());
            throw ServiceExceptions.payment("Failed to initialize SSLCommerz payment: " + e.getMessage());
        }
    }

    //Validate payment from SSLCommerz callback
    @Transactional
    public void validatePayment(String tranId, String valId, String status, String rawResponse) {
        SslCommerzPayment payment = sslCommerzPaymentRepository.findByTranId(tranId)
                .orElseThrow(() -> new ResourceNotFoundException("SSLCommerz payment", tranId));

        payment.setValidationId(valId);
        payment.setStatus(status);
        payment.setRawResponse(rawResponse);
        payment.setUpdatedAt(UserDateTimeUtil.now());

        sslCommerzPaymentRepository.save(payment);
    }

    //Validate payment using SSLCommerz validation API
    public boolean validatePaymentWithSslCommerz(String tranId, String valId) {
        try {
            Map<String, String> requestData = new HashMap<>();
            requestData.put("store_id", storeId);
            requestData.put("store_passwd", storePassword);
            requestData.put("val_id", valId);
            requestData.put("format", "json");

            String apiUrl = apiBaseUrl + "/validator/api/validationserverAPI.php";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            requestData.forEach(formData::add);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            log.info("SSLCommerz Validation Response: {}", response.getBody());

            // Update payment status based on validation response
            SslCommerzPayment payment = sslCommerzPaymentRepository.findByTranId(tranId)
                    .orElseThrow(() -> new ResourceNotFoundException("SSLCommerz payment", tranId));

            if (response.getBody() != null && response.getBody().contains("\"status\":\"VALID\"")) {
                payment.setStatus("VALIDATED");
                payment.setValidationId(valId);
                payment.setUpdatedAt(UserDateTimeUtil.now());
                sslCommerzPaymentRepository.save(payment);
                return true;
            } else {
                payment.setStatus("FAILED");
                payment.setUpdatedAt(UserDateTimeUtil.now());
                sslCommerzPaymentRepository.save(payment);
                return false;
            }

        } catch (Exception e) {
            log.error("Error validating SSLCommerz payment: {}", e.getMessage());
            return false;
        }
    }

    //Extract redirect URL from SSLCommerz response
    private String extractRedirectUrl(String response) {
        try {
            // Parse JSON response to extract GatewayPageURL
            if (response.contains("\"GatewayPageURL\"")) {
                String[] parts = response.split("\"GatewayPageURL\"");
                if (parts.length > 1) {
                    String urlPart = parts[1];
                    int start = urlPart.indexOf("\"") + 1;
                    int end = urlPart.indexOf("\"", start);
                    if (end != -1) {
                        return urlPart.substring(start, end);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting redirect URL: {}", e.getMessage());
            return null;
        }
    }

    //Extract session key from SSLCommerz response
    private String extractSessionKey(String response) {
        try {
            // Parse JSON response to extract sessionkey
            if (response.contains("\"sessionkey\"")) {
                String[] parts = response.split("\"sessionkey\"");
                if (parts.length > 1) {
                    String keyPart = parts[1];
                    int start = keyPart.indexOf("\"") + 1;
                    int end = keyPart.indexOf("\"", start);
                    if (end != -1) {
                        return keyPart.substring(start, end);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting session key: {}", e.getMessage());
            return null;
        }
    }

    // Create SSLCommerz wallet deposit session (similar to Stripe's createInstantDepositCheckoutSession)
    @Transactional
    public SslCommerzInitResponse createWalletDepositSession(Long userId, Double amount, String remarks) {
        try {
            // Validate amount early (avoid gateway rejection / empty amount)
            if (amount == null || amount <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Deposit amount must be greater than zero");
            }

            // Normalize to 2 decimals (SSLCommerz expects a proper number string)
            BigDecimal normalizedAmount = BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP);

            // Create wallet deposit record with a globally-unique reference
            String reference = referenceGeneratorService.nextReference("dp");

            WalletDeposit deposit = WalletDeposit.builder()
                    .userId(userId)
                    .type(DepositType.BANK_TRANSFER_OR_MFS)
                    .status(DepositStatus.INIT)
                    .amount(normalizedAmount.doubleValue())
                    .remarks(remarks)
                    .reference(reference)
                    .createdAt(UserDateTimeUtil.now())
                    .currency(Currency.BDT)
                    .build();

            walletDepositRepository.save(deposit);

            // Generate unique transaction ID for wallet deposit
            String tranId = "WD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

            // Create SSLCommerz payment record for wallet deposit
            SslCommerzPayment payment = SslCommerzPayment.builder()
                    .bookingId(null) // No booking for wallet deposits
                    .userId(userId)
                    .method(PaymentProvider.SSL_COMMERZ)
                    .tranId(tranId)
                    .amount(normalizedAmount)
                    .currency("BDT")
                    .status("INITIATED")
                    .depositReference(deposit.getReference())
                    .paymentType(SslCommerzPayment.PaymentType.WALLET_DEPOSIT)
                    .customerName("Wallet Deposit")
                    .customerEmail("wallet@tufantrip.com")
                    .customerPhone("01700000000")
                    .createdAt(UserDateTimeUtil.now())
                    .updatedAt(UserDateTimeUtil.now())
                    .build();

            sslCommerzPaymentRepository.save(payment);

            // Prepare SSLCommerz request for wallet deposit
            Map<String, String> requestData = new HashMap<>();
            requestData.put("store_id", storeId);
            requestData.put("store_passwd", storePassword);
            requestData.put("total_amount", normalizedAmount.toPlainString());
            requestData.put("amount", normalizedAmount.toPlainString());
            requestData.put("currency", "BDT");
            requestData.put("tran_id", tranId);
            requestData.put("success_url", successUrl + "&deposit_ref=" + deposit.getReference());
            requestData.put("fail_url", failUrl + "&deposit_ref=" + deposit.getReference());
            requestData.put("cancel_url", cancelUrl + "&deposit_ref=" + deposit.getReference());
            requestData.put("ipn_url", callbackUrl);
            requestData.put("emi_option", "0");
            requestData.put("cus_name", "Wallet Deposit");
            requestData.put("cus_email", "wallet@tufantrip.com");
            requestData.put("cus_phone", "01700000000");
            requestData.put("cus_add1", "Dhaka");
            requestData.put("cus_city", "Dhaka");
            requestData.put("cus_country", "Bangladesh");
            requestData.put("shipping_method", "NO");
            requestData.put("num_of_item", "1");
            requestData.put("product_name", "Wallet Deposit");
            requestData.put("product_category", "Financial");
            requestData.put("product_profile", "general");

            // Call SSLCommerz API
            String apiUrl = apiBaseUrl + "/gwprocess/v4/api.php";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            requestData.forEach(formData::add);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            log.info("SSLCommerz Wallet Deposit API Response: {}", response.getBody());

            String body = response.getBody();

            // Parse response
            if (body != null && body.contains("\"status\":\"SUCCESS\"")) {
                String redirectUrl = extractRedirectUrl(body);
                String sessionKey = extractSessionKey(body);

                // Update payment with session key
                payment.setSessionKey(sessionKey);
                payment.setRawResponse(body);
                sslCommerzPaymentRepository.save(payment);

                return SslCommerzInitResponse.builder()
                        .tranId(tranId)
                        .sessionKey(sessionKey)
                        .redirectUrl(redirectUrl)
                        .status("INITIATED")
                        .amount(normalizedAmount)
                        .currency("BDT")
                        .depositReference(deposit.getReference())
                        .build();
            }

            // Capture gateway failure reason for better debugging
            String failedReason = null;
            String gatewayStatus = null;
            try {
                if (body != null) {
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
                    gatewayStatus = node.path("status").asText(null);
                    failedReason = node.path("failedreason").asText(null);
                }
            } catch (Exception ignore) {
                // keep fallback message
            }

            payment.setRawResponse(body);
            payment.setStatus("FAILED");
            payment.setUpdatedAt(UserDateTimeUtil.now());
            sslCommerzPaymentRepository.save(payment);

            String msg = "SSLCommerz wallet deposit API call failed";
            if (gatewayStatus != null) msg += " (status=" + gatewayStatus + ")";
            if (failedReason != null && !failedReason.isBlank()) msg += ": " + failedReason;
            else if (body != null) msg += ": " + body;
            throw ServiceExceptions.payment(msg);

        } catch (Exception e) {
            log.error("Error creating SSLCommerz wallet deposit session: {}", e.getMessage());
            throw ServiceExceptions.payment("Failed to create SSLCommerz wallet deposit session: " + e.getMessage());
        }
    }


    // Simplified wallet deposit processing without validation ID requirement
    @Transactional
    public void processSuccessfulWalletDepositSimple(String tranId) {
        try {
            SslCommerzPayment payment = sslCommerzPaymentRepository.findByTranId(tranId)
                    .orElseThrow(() -> new ResourceNotFoundException("SSLCommerz wallet deposit payment", tranId));

            if (payment.getPaymentType() != SslCommerzPayment.PaymentType.WALLET_DEPOSIT) {
                throw ServiceExceptions.payment("Payment is not a wallet deposit: " + tranId);
            }

            if (payment.getDepositReference() == null) {
                throw ServiceExceptions.payment("No deposit reference found for payment: " + tranId);
            }

            // Find wallet deposit
            Optional<WalletDeposit> depositOpt = walletDepositRepository.findByReference(payment.getDepositReference());
            if (depositOpt.isEmpty()) {
                throw new ResourceNotFoundException("Wallet deposit", payment.getDepositReference());
            }

            WalletDeposit deposit = depositOpt.get();

            // Only process if still pending
            if (deposit.getStatus() == DepositStatus.PENDING) {
                // Update deposit status to approved
                deposit.setStatus(DepositStatus.APPROVED);
                deposit.setApprovedAt(UserDateTimeUtil.now());
                deposit.setApprovedBy(1L); // System approval
                deposit.setTransactionId(tranId); // Set the SSLCommerz transaction ID
                deposit.setRemarks(deposit.getRemarks() + " - SSLCommerz TranId: " + tranId);

                walletDepositRepository.save(deposit);

                log.info("For User ID: {}, Deposit Amount: {}", deposit.getUserId(), deposit.getAmount());

                creditWalletDepositLedger(deposit, deposit.getAmount(), tranId);

                log.info("Successfully processed SSLCommerz wallet deposit for tranId: {}", tranId);
            } else {
                log.warn("Wallet deposit {} already processed, status: {}", deposit.getReference(), deposit.getStatus());
            }

            // Update SSLCommerz payment status
            payment.setStatus("VALIDATED");
            payment.setUpdatedAt(UserDateTimeUtil.now());
            sslCommerzPaymentRepository.save(payment);

        } catch (Exception e) {
            log.error("Error processing SSLCommerz wallet deposit: {}", e.getMessage());
            throw ServiceExceptions.payment("Failed to process wallet deposit: " + e.getMessage());
        }
    }

    // Process SSLCommerz callback with complete payment details
    @Transactional
    public void processSslCommerzCallback(SslCommerzCallbackRequest callbackData) {
        try {
            log.info("Processing SSLCommerz callback for tranId: {}", callbackData.getTranId());

            SslCommerzPayment payment = sslCommerzPaymentRepository.findByTranId(callbackData.getTranId())
                    .orElseThrow(() -> new ResourceNotFoundException("SSLCommerz payment", callbackData.getTranId()));

            // Update payment with callback data
            payment.setValidationId(callbackData.getValId());
            payment.setStatus(callbackData.getStatus());
            payment.setBankTranId(callbackData.getBankTranId());
            payment.setCardType(callbackData.getCardType());
            payment.setCardNo(callbackData.getCardNo());
            payment.setCardIssuer(callbackData.getCardIssuer());
            payment.setCardBrand(callbackData.getCardBrand());
            payment.setCardIssuerCountry(callbackData.getCardIssuerCountry());
            payment.setStoreAmount(callbackData.getStoreAmount());
            payment.setVerifySign(callbackData.getVerifySign());
            payment.setVerifySignSha2(callbackData.getVerifySignSha2());
            payment.setRiskLevel(callbackData.getRiskLevel());
            payment.setRiskTitle(callbackData.getRiskTitle());
            payment.setUpdatedAt(UserDateTimeUtil.now());

            sslCommerzPaymentRepository.save(payment);

            // If status is VALID and this is a wallet deposit, process it
            if ("success".equals(callbackData.getStatus())) {
                log.info("Processing wallet deposit for tranId: {}", callbackData.getTranId());
                processWalletDepositFromCallback(payment);
            } else {
                log.warn("Payment status is not VALID for tranId: {}, status: {}",
                        callbackData.getTranId(), callbackData.getStatus());
            }

            log.info("Successfully processed SSLCommerz callback for tranId: {}", callbackData.getTranId());

        } catch (Exception e) {
            log.error("Error processing SSLCommerz callback for tranId {}: {}",
                    callbackData.getTranId(), e.getMessage(), e);
            throw ServiceExceptions.payment("Failed to process payment callback", e);
        }
    }

    // Helper method to process wallet deposit from callback
    private void processWalletDepositFromCallback(SslCommerzPayment payment) {
        try {
            if (payment.getDepositReference() == null) {
                throw ServiceExceptions.notFound("Deposit reference for payment: " + payment.getTranId());
            }

            Optional<WalletDeposit> depositOpt = walletDepositRepository.findByReference(payment.getDepositReference());
            if (depositOpt.isEmpty()) {
                throw new ResourceNotFoundException("Wallet deposit", payment.getDepositReference());
            }

            WalletDeposit deposit = depositOpt.get();

            // Only process if still pending
            if (deposit.getStatus() == DepositStatus.INIT) {
                deposit.setStatus(DepositStatus.APPROVED);
                deposit.setApprovedAt(UserDateTimeUtil.now());
                deposit.setApprovedBy(1L); // System approval
                deposit.setTransactionId(payment.getTranId());
                deposit.setRemarks(deposit.getRemarks() + " - SSLCommerz TranId: " + payment.getTranId() +
                        ", BankTranId: " + payment.getBankTranId());

                walletDepositRepository.save(deposit);

                //
                double finalAmount = currencyService.convertCurrency(
                        String.valueOf(payment.getAmount()),
                        payment.getCurrency(),
                        "BDT",
                        "Others"
                );


                log.info("For User ID: {}, Deposit Amount: {}", deposit.getUserId(), finalAmount);

                creditWalletDepositLedger(deposit, finalAmount, payment.getTranId());

                log.info("Successfully processed wallet deposit for tranId: {}", payment.getTranId());
            } else {
                log.warn("Wallet deposit {} already processed, status: {}",
                        deposit.getReference(), deposit.getStatus());
            }
        } catch (Exception e) {
            log.error("Error processing wallet deposit from callback: {}", e.getMessage(), e);
            throw ServiceExceptions.payment("Failed to process wallet deposit", e);
        }
    }

    /** Credits wallet balance and records a matching ledger transaction (same pattern as Stripe webhook). */
    private void creditWalletDepositLedger(WalletDeposit deposit, double creditAmount, String externalTranId) {
        walletService.addToWalletBalance(deposit.getUserId(), creditAmount);

        Transaction txn = Transaction.builder()
                .type(deposit.getType() != null ? deposit.getType().name() : DepositType.BANK_TRANSFER_OR_MFS.name())
                .amount(creditAmount)
                .currency(deposit.getCurrency() != null ? deposit.getCurrency().name() : "BDT")
                .convertedAmount(String.valueOf(creditAmount))
                .exchangeRate(deposit.getExchangeRate())
                .description("SSLCommerz deposit – Ref: " + deposit.getReference()
                        + (externalTranId != null ? ", TranId: " + externalTranId : ""))
                .userId(deposit.getUserId())
                .createdBy(String.valueOf(deposit.getUserId()))
                .createdAt(UserDateTimeUtil.now())
                .sourceType(TransactionSourceType.DEPOSIT.name())
                .sourceId(deposit.getId())
                .reference(deposit.getReference())
                .active(true)
                .build();
        transactionRepository.save(txn);
    }
}

