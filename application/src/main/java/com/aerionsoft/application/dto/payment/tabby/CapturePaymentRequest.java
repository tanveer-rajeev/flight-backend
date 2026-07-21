package com.aerionsoft.application.dto.payment.tabby;

import java.math.BigDecimal;

public record CapturePaymentRequest(
        BigDecimal amount
) {}