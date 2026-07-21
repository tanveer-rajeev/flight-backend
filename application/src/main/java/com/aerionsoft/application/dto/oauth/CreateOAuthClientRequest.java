package com.aerionsoft.application.dto.oauth;

import jakarta.validation.constraints.NotBlank;

public record CreateOAuthClientRequest(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        String description
) {}

