package com.aerionsoft.application.dto.payment.tabby;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TabbyWebhookPayload(
        String id,
        String status,
        BigDecimal amount,
        String currency,
        String createdAt,
        Order order,
        List<Capture> captures
) {
    public record Order(
            @JsonAlias("reference_id")
            String referenceId
    ) {}

    public record Capture(
            String id,
            BigDecimal amount,
            String createdAt
    ) {}
}
