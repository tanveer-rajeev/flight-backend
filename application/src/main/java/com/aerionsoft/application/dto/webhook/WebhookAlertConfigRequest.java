package com.aerionsoft.application.dto.webhook;

import com.aerionsoft.application.enums.webhook.WebhookAlertType;
import com.aerionsoft.application.enums.webhook.WebhookChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookAlertConfigRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @NotNull(message = "Alert type is required")
    private WebhookAlertType alertType;

    @NotNull(message = "Channel is required")
    private WebhookChannel channel;

    @NotBlank(message = "Webhook URL is required")
    @Size(max = 2000, message = "Webhook URL must be at most 2000 characters")
    private String webhookUrl;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    private Boolean isActive;
}
