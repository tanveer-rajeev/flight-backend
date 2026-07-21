package com.aerionsoft.application.dto.payment.tabby;

import com.aerionsoft.application.enums.payment.PaymentStatus;

public record TabbyCheckoutResult(String paymentId, PaymentStatus status, String redirectUrl) {
}
