package com.aerionsoft.application.dto.payment.tabby;

import java.math.BigDecimal;

public record RefundPaymentRequest(
        BigDecimal amount,
        String reason
) {}
