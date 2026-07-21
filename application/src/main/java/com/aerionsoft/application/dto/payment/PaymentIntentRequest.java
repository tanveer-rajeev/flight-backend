package com.aerionsoft.application.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentIntentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount; // Amount in cents

    @NotNull(message = "Currency is required")
    private String currency = "USD";

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private String description;

    // Customer information
    private String customerEmail;
    private String customerName;

    // Optional metadata
    private String flightNumber;
    private String pnr;
}
