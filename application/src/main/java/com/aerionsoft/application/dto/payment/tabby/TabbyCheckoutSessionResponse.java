package com.aerionsoft.application.dto.payment.tabby;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TabbyCheckoutSessionResponse(
        String id,
        String status,
        String token,
        Configuration configuration,
        Payment payment,
        MerchantUrls merchantUrls
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Configuration(
            Map<String, List<AvailableProduct>> availableProducts,
            Map<String, ProductStatus> products
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AvailableProduct(
            List<Installment> installments,
            String webUrl,
            String qrCode,
            String originalType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Installment(
            LocalDate dueDate,
            BigDecimal oldAmount,
            BigDecimal amount,
            BigDecimal principal,
            BigDecimal serviceFee
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProductStatus(
            String type,
            boolean isAvailable,
            String rejectionReason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Payment(
            String id,
            OffsetDateTime createdAt,
            String status,
            boolean isTest,
            BigDecimal amount,
            String currency,
            String description
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MerchantUrls(
            String success,
            String cancel,
            String failure
    ) {}
}