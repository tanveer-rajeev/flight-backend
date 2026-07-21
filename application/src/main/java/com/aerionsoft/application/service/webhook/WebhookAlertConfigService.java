package com.aerionsoft.application.service.webhook;

import com.aerionsoft.application.dto.webhook.WebhookAlertConfigRequest;
import com.aerionsoft.application.dto.webhook.WebhookAlertConfigResponse;
import com.aerionsoft.application.dto.webhook.WebhookAlertMetaResponse;

import java.util.List;

public interface WebhookAlertConfigService {

    WebhookAlertConfigResponse create(WebhookAlertConfigRequest request, Long adminUserId);

    WebhookAlertConfigResponse update(Long id, WebhookAlertConfigRequest request, Long adminUserId);

    WebhookAlertConfigResponse getById(Long id);

    List<WebhookAlertConfigResponse> getAll(Boolean activeOnly);

    void delete(Long id);

    WebhookAlertMetaResponse getMeta();

    void sendTestAlert(Long id);
}
