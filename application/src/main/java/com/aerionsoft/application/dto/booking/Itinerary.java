package com.aerionsoft.application.dto.booking;

import com.aerionsoft.application.enums.user.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class Itinerary {

    @NotBlank(message = "title name is required")
    private String title;
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "Mobile is required")
    @Pattern(regexp = "^\\d{7,15}$", message = "Mobile must be 7-15 digits")
    private String mobile;
    @NotBlank(message = "Mobile country code is required")
    private String mobileCountryCode;
    @NotBlank(message = "Date of birth is required")
    private String dob;
    @NotNull(message = "Gender is required")
    private Gender gender;
    @Email(message = "Email should be valid")
    private String email;
    @NotBlank(message = "Passport number is required")
    private String passportNo;
    @NotBlank(message = "Passport issue date is required")
    private String passportIssueDate;
    @NotBlank(message = "Passport expiry date is required")
    private String passportExpiryDate;
    @NotBlank(message = "Country name is required")
    private String countryName;
    @NotBlank(message = "Country code is required")
    private String countryCode;
    @NotBlank(message = "City name is required")
    private String cityName;
    @NotBlank(message = "City code is required")
    private String cityCode;
    @NotBlank(message = "Meal code is required")
    private String mealCode;

    @NotBlank(message = "Address line 1 is required")

    private String addressLine1;
    private String addressLine2;
    private String nationality;

}
