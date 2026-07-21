package com.aerionsoft.application.dto.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SslCommerzInitRequest {
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String currency = "BDT";
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @Email(message = "Valid email is required")
    @NotBlank(message = "Customer email is required")
    private String customerEmail;
    
    @NotBlank(message = "Customer phone is required")
    private String customerPhone;
}