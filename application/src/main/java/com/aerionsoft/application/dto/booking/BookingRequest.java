package com.aerionsoft.application.dto.booking;

import com.aerionsoft.application.dto.flight.search.extras.FinalFare;
import com.aerionsoft.application.dto.flight.search.extras.PackageBaggage;
import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.enums.booking.BookType;
import com.aerionsoft.application.enums.booking.BookingClass;
import com.aerionsoft.application.enums.booking.BookingType;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.enums.booking.TripType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    @NotNull(message = "Trip Type is required")
    private TripType tripType;

    @NotNull(message = "Book Type is required")
    private BookType bookType;

    @NotNull(message = "Provider name is required")
    private Provider providerName;
    @NotNull(message = "bookingClass is required")
    private BookingClass bookingClass;

    @NotNull(message = "Booking type is required")
    private BookingType type;
    private String description;

    @NotNull(message = "channel is required")
    private String channel;

    private List<Long> travellerIds;

    private String resultIndex;

    private String bundleCode;

    @Valid
    private List<TravellerRequest> itineraries;

    @NotNull(message = "Fare Rule is required")
    private FinalFare fare;

    @NotNull(message = "Booking Allowed is required")
    @JsonProperty("isBookingAllowed")
    private Boolean isBookingAllowed;

    @NotNull(message = "Ticketing Allowed is required")
    @JsonProperty("isTicketingAllowed")
    private Boolean isTicketingAllowed;

    // Flight segments
    @NotEmpty(message = "Segments are required")
    private List<SegmentRequest> segments;

    private List<FlightSSR> flightSSRList;

    private List<PackageBaggage> packageBaggageList;

    private String timeOffset;

    private String groupTicketType;


}


