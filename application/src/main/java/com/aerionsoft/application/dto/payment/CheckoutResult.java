package com.aerionsoft.application.dto.payment;

import com.aerionsoft.application.enums.payment.PaymentStatus;

public record CheckoutResult(String paymentId, PaymentStatus paymentStatus,String redirectUrl) {
}
