package com.aerionsoft.application.dto.client.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    @NotNull
    private String fullName;
    @NotBlank
    private String phoneNumber;
    private Double balance;
    private String passportNumber;
    private String passportExpiryDate;
    private String currency; // Default is "USD"
    @NotNull(message = "Date of birth is required")
    private LocalDate dob; // Date of birth in ISO format (yyyy-MM-dd)
    @NotBlank
    private String address;
    @NotBlank
    private String nationality;
}