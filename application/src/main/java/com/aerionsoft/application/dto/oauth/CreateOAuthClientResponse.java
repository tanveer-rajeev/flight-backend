package com.aerionsoft.application.dto.oauth;

public record CreateOAuthClientResponse(
        String clientId,
        String clientSecret,
        String description,
        boolean active
) {}

