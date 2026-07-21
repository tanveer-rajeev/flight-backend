package com.aerionsoft.application.dto.client.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserRequest {

    @Email(message = "Email must be a valid email address")
    @Size(max = 100)
    private String email;

    @Size(max = 15)
    private String phoneNumber;

    @Size(max = 100)
    private String fullName;

    @Size(max = 255)
    private String address;

    private String currency;

    private Double balance;

    private String passportNumber;

    private String passportExpiryDate;

    private String image;

    private LocalDate dob;

    private String nationality;

    private String ticketPreview;

    private String invoicePreview;

    private String moneyReceiptPreview;
}
