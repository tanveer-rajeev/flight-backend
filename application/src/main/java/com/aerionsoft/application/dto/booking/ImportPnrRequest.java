package com.aerionsoft.application.dto.booking;

import com.aerionsoft.application.dto.booking.core.CoreResponse;
import com.aerionsoft.application.dto.flight.search.extras.FinalFare;
import com.aerionsoft.application.dto.flight.search.extras.PackageBaggage;
import com.aerionsoft.application.dto.flight.validation.FareDesc;
import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.enums.booking.BookingClass;
import com.aerionsoft.application.enums.booking.BookingType;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.enums.booking.TripType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportPnrRequest {
    // Supplier ID to be passed in the request body
    private Long supplierId;

    @NotNull(message = "Agency Id is required")
    private Long agencyId;

    @NotNull(message = "Trip Type is required")
    private TripType tripType;

    @NotNull(message = "Provider name is required")
    private Provider providerName;

    @NotNull(message = "Booking class is required")
    private BookingClass bookingClass;

    @NotNull(message = "Booking type is required")
    private BookingType type;

    private String description;

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotNull(message = "Booking price is required")
    @Positive(message = "Booking price must be positive")
    private Double bookingPrice;

    private Double buyPrice;

    private Double originalPrice;

    private Double markupAmount;

    private Double taxAmount;

    private String currency;

    private List<Long> travellerIds;

    @Valid
    private List<TravellerRequest> itineraries;

    @Valid
    private List<SegmentRequest> segments;

    private FinalFare fare;

    @Builder.Default
    @JsonProperty("isBookingAllowed")
    private boolean isBookingAllowed = true;

    @Builder.Default
    @JsonProperty("isTicketingAllowed")
    private boolean isTicketingAllowed = true;

    @Builder.Default
    private boolean deductFromWallet = true;

    private String reason;

    private String timeOffset;

    private List<PackageBaggage> packageBaggageList;

    @Valid
    @NotNull(message = "Core response is required")
    private CoreResponse coreResponse;

    private FareDesc fareDesc;

}

