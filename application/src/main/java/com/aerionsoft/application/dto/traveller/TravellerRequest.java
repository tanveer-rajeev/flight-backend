package com.aerionsoft.application.dto.traveller;

import com.aerionsoft.application.enums.user.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Data
public class TravellerRequest {

    @NotBlank(message = "Title name is required")
    private String title;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;


    private String mobile;

    @NotBlank(message = "Mobile country code is required")
    private String mobileCountryCode;

    @NotBlank(message = "Date of birth is required")
    private String dob;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private String email;

    private String passportNo;


    private String passportIssueDate = "2020-01-01";

    private String passportExpiryDate;

    private String countryName;

    private String countryCode;

    private String cityName;

    private String cityCode;

    private String mealCode;

    private String addressLine1 = "Dubai";

    private String addressLine2 = "Dubai";
    @NotBlank(message = "Nationality is required")
    private String nationality;
    private String passportImageUrl;
    private String visaOrNidImageURL;


    public String calculatePassengerType() {
        LocalDate birthDate = LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int age = Period.between(birthDate, LocalDate.now()).getYears();

        return age < 2 ? "INF" : age < 12 ? "CHD" : "ADT";
    }
}