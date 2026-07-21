package com.aerionsoft.application.dto.traveller;

import com.aerionsoft.application.enums.user.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TravellerResponse {
    private Long id;
    private String title;
    private String firstName;
    private String lastName;
    private String mobile;
    private String mobileCountryCode;
    private String email;
    private Gender gender;
    private LocalDate dob;
    private String passportNo;
    private LocalDate passportIssueDate;
    private LocalDate passportExpiryDate;
    private String countryName;
    private String countryCode;
    private String cityName;
    private String cityCode;
    private String mealCode;
    private String nationality;
    private String addressLine1;
    private String addressLine2;
    private String visaOrNidImageURL;
    private String passportImageUrl;
    private String ticketNumber;

}
