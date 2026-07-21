package com.aerionsoft.application.dto.client.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @Size(max = 15)
    private String phoneNumber;

    @Size(max = 255)
    private String address;

    private String currency = "AED";

    private Double balance;

    private String passportNumber;

    private String passportExpiryDate;

    private String image;

    private LocalDate dob;

    private String nationality;
}
