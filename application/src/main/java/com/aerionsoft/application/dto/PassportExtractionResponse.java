package com.aerionsoft.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PassportExtractionResponse {

    /** Cloudflare R2 URL where the uploaded passport image is stored. */
    private String imageUrl;

    private String surname;
    private String givenNames;
    private String passportNumber;
    private String nationality;
    private String dateOfBirth;
    private String dateOfBirthIso;
    private String dateOfExpiry;
    private String dateOfExpiryIso;
    private String dateOfIssue;
    private String dateOfIssueIso;
    private String gender;
    private String placeOfBirth;
    private String issuingCountry;
    private String mrzLine1;
    private String mrzLine2;

    /** True when AI extraction succeeded and fields were parsed. */
    private boolean extracted;

    /** Raw AI response text — populated when JSON parsing fails. */
    private String rawExtraction;
}
