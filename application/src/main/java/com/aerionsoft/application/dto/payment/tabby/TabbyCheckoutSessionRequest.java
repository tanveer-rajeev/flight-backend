package com.aerionsoft.application.dto.payment.tabby;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TabbyCheckoutSessionRequest(
        Payment payment,
        String lang,
        String merchantCode,
        MerchantUrls merchantUrls
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Payment(
            BigDecimal amount,
            String currency,
            Buyer buyer,
            Order order,
            String referenceId
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Buyer(
            String phone,
            String email
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Order(
            String referenceId
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MerchantUrls(
            String success,
            String cancel,
            String failure
    ) {}
}