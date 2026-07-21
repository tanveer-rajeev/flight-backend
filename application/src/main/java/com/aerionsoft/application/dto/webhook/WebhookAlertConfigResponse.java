package com.aerionsoft.application.dto.webhook;

import com.aerionsoft.application.enums.webhook.WebhookAlertType;
import com.aerionsoft.application.enums.webhook.WebhookChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookAlertConfigResponse {

    private Long id;
    private String name;
    private WebhookAlertType alertType;
    private String alertTypeLabel;
    private WebhookChannel channel;
    private String channelLabel;
    private String webhookUrl;
    private String description;
    private Boolean isActive;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
