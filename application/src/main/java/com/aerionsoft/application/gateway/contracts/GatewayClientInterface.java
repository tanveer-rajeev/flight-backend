package com.aerionsoft.application.gateway.contracts;

import com.aerionsoft.application.dto.payment.PaymentRequestDto;
import com.aerionsoft.application.entity.paymentGateway.Payment;

public interface GatewayClientInterface {
    String getGatewayId();

    Object processPayment(PaymentRequestDto request, Payment payment);

    Object getOrderStatus(String orderReference);
}
