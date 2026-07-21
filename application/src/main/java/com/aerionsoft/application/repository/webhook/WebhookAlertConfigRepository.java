package com.aerionsoft.application.repository.webhook;

import com.aerionsoft.application.entity.webhook.WebhookAlertConfig;
import com.aerionsoft.application.enums.webhook.WebhookAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookAlertConfigRepository extends JpaRepository<WebhookAlertConfig, Long> {

    List<WebhookAlertConfig> findByAlertTypeAndIsActiveTrueOrderByIdAsc(WebhookAlertType alertType);

    List<WebhookAlertConfig> findByIsActiveTrueOrderByAlertTypeAscIdAsc();

    List<WebhookAlertConfig> findAllByOrderByAlertTypeAscIdAsc();
}
