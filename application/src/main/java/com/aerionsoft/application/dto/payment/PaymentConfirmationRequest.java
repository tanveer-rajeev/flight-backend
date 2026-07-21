package com.aerionsoft.application.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentConfirmationRequest {

    @NotNull(message = "Payment Intent ID is required")
    private String paymentIntentId;

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private String paymentMethodId;
}
