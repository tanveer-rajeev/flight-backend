package com.aerionsoft.application.dto.flight.validation;

import lombok.Data;
import java.util.List;

@Data
public class PriceValidationRequest {
    private String resultIndex;
    private String providerName;
    private String channel;
    private List<BookInfo> bookInfoList;
    private String bundleCode;

    @Data
    public static class BookInfo {
        private String title;
        private String firstName;
        private String lastName;
        private String mobile;
        private String mobileCountryCode;
        private String dob;
        private String gender;
        private String email;
        private String passportNo;
        private String passportIssueDate;
        private String passportExpiryDate;
        private String countryName;
        private String countryCode;
        private String cityName;
        private String cityCode;
        private String addressLine1;
        private String addressLine2;
        private String mealCode;
        private String nationality;
        private String bookingClass;
    }

    private List<FlightSSR> flightSSRList;
}

