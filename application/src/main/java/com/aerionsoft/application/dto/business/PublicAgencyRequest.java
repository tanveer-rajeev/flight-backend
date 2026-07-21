package com.aerionsoft.application.dto.business;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;

@Data
public class PublicAgencyRequest {
    @NotBlank(message = "Company name is required")
    private String companyName;

    private String companyEmail;
    private String companyAddress;
    private String companyPhone;
    private String companyLogo;
    private String companyLicence;
    private String civilAviationCertNumber;
    private Date civilAviationCertExpiryDate;
    private String addressProof;
    private String attachment;

    @NotBlank(message = "Representative name is required")
    private String representativeName;

    private String representativeMobile;

    @NotBlank(message = "Representative email is required")
    @Email(message = "Representative email must be a valid email address")
    private String representativeEmail;

    private String representativePosition;
    private String digitalSignature;

    @NotBlank(message = "Password is required")
    private String password;

    /** Optional currency code; defaults to USD. */
    private String currency;
}
