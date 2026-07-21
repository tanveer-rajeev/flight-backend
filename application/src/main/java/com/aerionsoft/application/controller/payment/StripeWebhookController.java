package com.aerionsoft.application.controller.payment;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.admin.StripeWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class StripeWebhookController {

    @Autowired
    private StripeWebhookService stripeWebhookService;

    @PostMapping("/api/webhooks/stripe")
    public ResponseEntity<BaseResponse<Void>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        stripeWebhookService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok(BaseResponse.ok("Webhook processed successfully"));
    }
}
