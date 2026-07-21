package com.aerionsoft.application.dto.payment.tabby;

import java.math.BigDecimal;
import java.util.List;

public record TabbyCheckoutCommand(
        Long userId,
        String referenceId,
        BigDecimal amount,
        String currency,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        String paymentType
) { }
