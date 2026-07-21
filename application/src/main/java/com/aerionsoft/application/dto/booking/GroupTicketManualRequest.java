package com.aerionsoft.application.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Request DTO for creating a manual booking from a group ticket.
 * Segment information is automatically fetched from the GroupTicket entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTicketManualRequest {

    @NotBlank(message = "GF code is required")
    private String gfCode;

    @NotNull(message = "Agency ID is required")
    private Long agencyId;

    @Valid
    @NotNull(message = "Fare is required")
    private GroupTicketFare fare;

    @Valid
    @NotEmpty(message = "At least one traveler is required")
    private List<GroupTicketTraveler> travelers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupTicketFare {
        @NotNull(message = "Base fare is required")
        private Double baseFare;

        @NotNull(message = "Tax is required")
        private Double tax;

        @NotBlank(message = "Currency is required")
        private String currency;

        @NotNull(message = "Total fare is required")
        private Double totalFare;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupTicketTraveler {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        @NotBlank(message = "Gender is required")
        private String gender;

        @NotBlank(message = "Date of birth is required")
        private String dob;

        private String passportNo;
        private String passportIssueDate;
        private String passportExpiryDate;
        private String nationality;
        private String visaOrNidImageURL;
        private String passportImageUrl;
        private String mobileCountryCode;
    }
}

