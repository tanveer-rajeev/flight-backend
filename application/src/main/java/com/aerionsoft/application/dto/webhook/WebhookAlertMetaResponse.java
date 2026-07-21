package com.aerionsoft.application.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookAlertMetaResponse {

    private List<WebhookAlertOptionDTO> alertTypes;
    private List<WebhookAlertOptionDTO> channels;
}
