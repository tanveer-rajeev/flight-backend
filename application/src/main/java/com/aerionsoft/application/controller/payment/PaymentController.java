package com.aerionsoft.application.controller.payment;

import com.aerionsoft.application.controller.BaseController;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.payment.*;
import com.aerionsoft.application.entity.paymentGateway.SslCommerzPayment;
import com.aerionsoft.application.repository.payment.SslCommerzPaymentRepository;
import com.aerionsoft.application.service.payment.SslCommerzService;
import com.aerionsoft.application.service.admin.StripePaymentService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.Optional;

@RestController
@Validated
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController extends BaseController {



    @Autowired
    private StripePaymentService stripePaymentService;
    
    @Autowired
    private SslCommerzService sslCommerzService;
    
    @Autowired
    private SslCommerzPaymentRepository sslCommerzPaymentRepository;



    @PostMapping("/success")
    public RedirectView paymentSuccess(@RequestParam Map<String, String> params) {
        try {
            log.info("SSLCommerz success callback received with params: {}", params);

            // Map the parameters to our DTO
            SslCommerzCallbackRequest callbackRequest = SslCommerzCallbackRequest.builder()
                    .tranId(params.get("tran_id"))
                    .valId(params.get("val_id"))
                    .amount(params.get("amount") != null ? new java.math.BigDecimal(params.get("amount")) : null)
                    .cardType(params.get("card_type"))
                    .storeAmount(params.get("store_amount") != null ? new java.math.BigDecimal(params.get("store_amount")) : null)
                    .cardNo(params.get("card_no"))
                    .bankTranId(params.get("bank_tran_id"))
                    .status(params.get("status"))
                    .tranDate(params.get("tran_date"))
                    .error(params.get("error"))
                    .currency(params.get("currency"))
                    .cardIssuer(params.get("card_issuer"))
                    .cardBrand(params.get("card_brand"))
                    .cardSubBrand(params.get("card_sub_brand"))
                    .cardIssuerCountry(params.get("card_issuer_country"))
                    .cardIssuerCountryCode(params.get("card_issuer_country_code"))
                    .storeId(params.get("store_id"))
                    .verifySign(params.get("verify_sign"))
                    .verifyKey(params.get("verify_key"))
                    .verifySignSha2(params.get("verify_sign_sha2"))
                    .currencyType(params.get("currency_type"))
                    .currencyAmount(params.get("currency_amount") != null ? new java.math.BigDecimal(params.get("currency_amount")) : null)
                    .currencyRate(params.get("currency_rate") != null ? new java.math.BigDecimal(params.get("currency_rate")) : null)
                    .baseFair(params.get("base_fair") != null ? new java.math.BigDecimal(params.get("base_fair")) : null)
                    .valueA(params.get("value_a"))
                    .valueB(params.get("value_b"))
                    .valueC(params.get("value_c"))
                    .valueD(params.get("value_d"))
                    .subscriptionId(params.get("subscription_id"))
                    .riskLevel(params.get("risk_level"))
                    .riskTitle(params.get("risk_title"))
                    .build();

            // Process the callback through service layer
            sslCommerzService.processSslCommerzCallback(callbackRequest);

            log.info("Payment callback processed successfully for Tran ID: {}", callbackRequest.getTranId());

            // Get deposit reference from params if available (for wallet deposits)
            String depositRef = params.get("deposit_ref");
            if (depositRef != null && !depositRef.isEmpty()) {
                return new RedirectView("https://kingstartravel.com/payment/result?status=success&deposit_ref=" + depositRef);
            } else {
                return new RedirectView("https://kingstartravel.com/payment/result?status=success&tranId=" + callbackRequest.getTranId());
            }

        } catch (Exception e) {
            log.error("Error processing payment success callback: {}", e.getMessage(), e);
            return new RedirectView("https://kingstartravel.com/payment/result?status=failed");
        }
    }

    @PostMapping("/confirm")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'payment-confirm')") // admin or user
    public ResponseEntity<BaseResponse<PaymentIntentResponse>> confirmPayment(
            @Valid @RequestBody PaymentConfirmationRequest request) throws StripeException {
        PaymentIntentResponse response = stripePaymentService.confirmPayment(request);
        return ResponseEntity.ok(BaseResponse.ok("Payment confirmed successfully", response));
    }

    @GetMapping("/status/{paymentIntentId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-payment-status')") // admin or user
    public ResponseEntity<BaseResponse<PaymentIntentResponse>> getPaymentStatus(
            @PathVariable String paymentIntentId) throws StripeException {
        PaymentIntentResponse response = stripePaymentService.getPaymentStatus(paymentIntentId);
        return ResponseEntity.ok(BaseResponse.ok("Payment status retrieved successfully", response));
    }

    // SSLCommerz Payment Endpoints
    @PostMapping("/ssl/init")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'ssl-payment-init')") // admin or user
    public ResponseEntity<BaseResponse<SslCommerzInitResponse>> initSslPayment(
            @Valid @RequestBody SslCommerzInitRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        SslCommerzInitResponse response = sslCommerzService.initPayment(request, userId);
        return ResponseEntity.ok(BaseResponse.ok("SSLCommerz payment initiated", response));
    }

    @PostMapping("/ssl/validate")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'validate-ssl-payment')") // admin or user
    public ResponseEntity<BaseResponse<String>> validateSslPayment(@Valid @RequestBody SslCommerzValidateRequest request) {
        sslCommerzService.validatePayment(request.getTranId(), request.getValId(), request.getStatus(), request.getRawResponse());
        return ResponseEntity.ok(BaseResponse.ok("SSLCommerz payment validated"));
    }

    /**
     * Payment result endpoint - returns JSON response for frontend handling
     * This endpoint is PUBLIC (no authentication required) as SSLCommerz redirects here
     * Automatically processes wallet deposits when redirected from successful payment
     */
    @GetMapping("/payment/result")
    public ResponseEntity<BaseResponse<PaymentResultDto>> handlePaymentResult(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deposit_ref) {
        log.info("Payment result page accessed with status: {} and deposit_ref: {}", status, deposit_ref);

        // Auto-process wallet deposit if this is a successful wallet deposit
        // doing it because sslCommerz callback does not work properly
        if ("success".equals(status) && deposit_ref != null && !deposit_ref.isEmpty()) {
            try {
                log.info("Attempting to auto-process wallet deposit for reference: {}", deposit_ref);

                Optional<SslCommerzPayment> paymentOpt = sslCommerzPaymentRepository.findByDepositReference(deposit_ref);
                if (paymentOpt.isPresent()) {
                    SslCommerzPayment payment = paymentOpt.get();
                    log.info("Found SSLCommerz payment - TranId: {}, Status: {}, PaymentType: {}",
                            payment.getTranId(), payment.getStatus(), payment.getPaymentType());

                    if ("INITIATED".equals(payment.getStatus())) {
                        log.info("Processing wallet deposit for tranId: {}", payment.getTranId());
                        sslCommerzService.processSuccessfulWalletDepositSimple(payment.getTranId());

                        log.info("Successfully auto-processed wallet deposit for tranId: {} and deposit_ref: {}",
                                payment.getTranId(), deposit_ref);
                    } else {
                        log.info("Wallet deposit {} already processed, status: {}", deposit_ref, payment.getStatus());
                    }
                } else {
                    log.warn("No SSLCommerz payment found for deposit reference: {}", deposit_ref);
                }
            } catch (Exception e) {
                log.error("Error auto-processing wallet deposit for reference {}: {}", deposit_ref, e.getMessage(), e);
                // Don't fail the entire request, just log the error
            }
        }

        String title, message, cssClass;

        if ("success".equals(status)) {
            cssClass = "success";
            title = "Payment Successful!";
            message = "Your payment has been processed successfully. Thank you for your business!";
        } else if ("failed".equals(status)) {
            cssClass = "error";
            title = "Payment Failed";
            message = "Your payment could not be processed. Please try again or contact support.";
        } else if ("cancel".equals(status)) {
            cssClass = "info";
            title = "Payment Cancelled";
            message = "You have cancelled the payment process. No charges have been made.";
        } else {
            cssClass = "info";
            title = "Payment Status Unknown";
            message = "The payment status could not be determined. Please contact support for assistance.";
        }

        PaymentResultDto result = PaymentResultDto.builder()
                .status(status)
                .title(title)
                .message(message)
                .cssClass(cssClass)
                .build();

        return ResponseEntity.ok(BaseResponse.ok("Payment result retrieved", result));
    }
}
