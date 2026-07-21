package com.aerionsoft.application.dto.client.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrationRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;
    @NotBlank(message = "Password is required")
    private String password;
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Mobile is required")
    private String phone;

    @NotBlank(message = "Currency is required")
    private String currencyCode;
}