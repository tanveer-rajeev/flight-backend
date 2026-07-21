package com.aerionsoft.application.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private Long userId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private double amount;

    @NotNull(message = "Converted amount is required")
    @Positive(message = "Converted amount must be positive")
    private double exchangeRate;

    private String currency = "AED";

    @NotNull(message = "Gateway is required")
    private String gateway;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;
}
