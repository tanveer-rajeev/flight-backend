package com.aerionsoft.application.controller.payment;

import com.aerionsoft.application.dto.payment.tabby.TabbyWebhookPayload;
import com.aerionsoft.application.service.payment.tabby.TabbyWebhookProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/webhooks/tabby")
@RequiredArgsConstructor
@Slf4j
public class TabbyWebhookController {

    private final TabbyWebhookProcessor tabbyWebhookProcessor;

    @Value("${tabby.webhook-header-value}")
    private String expectedHeaderValue;

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader("X-Custom-Header") String receivedHeaderValue,
            @RequestBody TabbyWebhookPayload payload) {

        log.info("Received Tabby webhook: id={}, status={}", payload.id(), payload.status());

        if (!expectedHeaderValue.equals(receivedHeaderValue)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        tabbyWebhookProcessor.processAsync(payload);
        return ResponseEntity.ok().build();
    }
}