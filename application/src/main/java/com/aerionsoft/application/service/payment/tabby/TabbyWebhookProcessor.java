package com.aerionsoft.application.service.payment.tabby;

import com.aerionsoft.application.dto.payment.tabby.TabbyWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class TabbyWebhookProcessor{
    private final TabbyPaymentService tabbyPaymentService;

    @Async("taskExecutor")
    public void processAsync(TabbyWebhookPayload payload){
        log.info("Processing webhook async for payment {}", payload.id());
        try {
            tabbyPaymentService.handleWebhook(payload);
        } catch (Exception ex) {
            log.error("Failed to process Tabby webhook for payment id={}, status={}", payload.id(), payload.status(), ex);
        }
    }
}
