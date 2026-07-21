package com.aerionsoft.application.service.webhook;

import com.aerionsoft.application.dto.webhook.WebhookAlertMessage;
import com.aerionsoft.application.entity.webhook.WebhookAlertConfig;
import com.aerionsoft.application.enums.webhook.WebhookAlertType;
import com.aerionsoft.application.repository.webhook.WebhookAlertConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookAlertAsyncDispatcher {

    private final WebhookAlertConfigRepository repository;
    private final WebhookOutboundSender outboundSender;

    @Async("taskExecutor")
    public void dispatch(WebhookAlertType alertType, WebhookAlertMessage message) {
        List<WebhookAlertConfig> configs = repository.findByAlertTypeAndIsActiveTrueOrderByIdAsc(alertType);
        if (configs.isEmpty()) {
            log.info("No active webhook configs for alert type {}", alertType);
            return;
        }
        for (WebhookAlertConfig config : configs) {
            outboundSender.send(config, message);
        }
    }
}
