package com.aerionsoft.application.dto.payment.tabby;

import com.aerionsoft.application.enums.payment.PaymentStatus;

import java.math.BigDecimal;

public record TabbyPaymentDetailsResponse(
        String id,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String createdAt
) {}
