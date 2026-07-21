package com.aerionsoft.application.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationFromTemplateRequest {

    @NotBlank(message = "Template code is required")
    private String templateCode;

    private Long userId;
    private Long businessId;

    @NotEmpty(message = "Variables are required")
    private Map<String, String> variables;

    private Long createdBy;
}

