package com.aerionsoft.application.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookAlertMessage {

    private String title;
    private String body;
    private Map<String, String> fields;
}
