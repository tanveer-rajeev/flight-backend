package com.aerionsoft.application.dto.booking;

import com.aerionsoft.application.dto.flight.search.extras.FinalFare;
import com.aerionsoft.application.dto.flight.search.extras.PackageBaggage;
import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.enums.booking.BookingClass;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.BookingType;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.enums.booking.TripType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Request DTO for creating manual ticket bookings
 * Used when creating bookings directly without going through external GDS systems
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualBookingRequest {

    @NotNull(message = "Agency Id is required")
    private Long agencyId;

    @NotNull(message = "Trip Type is required")
    private TripType tripType;

    private Provider providerName;

    @NotNull(message = "Booking class is required")
    private BookingClass bookingClass;

    @NotNull(message = "Booking type is required")
    private BookingType type;

    private String description;

    private String channel;

    // PNR from external system (if any)
    private String pnr;

    // Ticket number (required for manual ticketed bookings)
    private String ticketNo;

    // Airline code/name
    @NotBlank(message = "Airline is required")
    private String airline;

    // Booking status for manual booking
    @NotNull(message = "Booking status is required")
    private BookingStatus status;

    // Booking price (required for manual bookings)
    @NotNull(message = "Booking price is required")
    @Positive(message = "Booking price must be positive")
    private Double bookingPrice;

    // Original price (GDS / published price)
    @Positive(message = "Original price must be positive")
    private Double originalPrice;

    // Supplier buy price (defaults to originalPrice when omitted)
    @Positive(message = "Buy price must be positive")
    private Double buyPrice;

    // Markup amount
    private Double markupAmount;

    // Tax amount
    private Double taxAmount;

    // Currency
    private String currency;

    // Last payment date (for hold bookings)
    private String lastPaymentDate;

    // Existing traveller IDs
    private List<Long> travellerIds;

    // New travellers to create
    @Valid
    @NotEmpty(message = "At least one traveller is required")
    private List<TravellerRequest> itineraries;

    // Flight segments
    @Valid
    private List<SegmentRequest> segments;

    // Fare details
    private FinalFare fare;

    // Booking date (defaults to today if not provided)
    private OffsetDateTime bookingDate;

    // Whether booking is allowed
    @Builder.Default
    @JsonProperty("isBookingAllowed")
    private boolean isBookingAllowed = true;

    // Whether ticketing is allowed
    @Builder.Default
    @JsonProperty("isTicketingAllowed")
    private boolean isTicketingAllowed = true;

    // Brand currency
    private String brandCurrency;

    // Brand exchange rate
    private Double brandExchangeRate;

    // Deduct from wallet (default true for manual bookings)
    @Builder.Default
    private boolean deductFromWallet = true;

    // Reason/remarks for the booking
    private String reason;

    // Time zone offset (e.g., +05:30, -08:00)
    private String timeOffset;

    // Package baggage list
    private List<PackageBaggage> packageBaggageList;

    /**
     * Optional admin supplier id for auto-created payable invoice after the booking is saved.
     * When omitted, the default admin supplier is used. Provider/channel are not used for manual bookings.
     */
    private Long supplierId;

    private String sourceType= "MANUAL";

    /** GROUP / UMRAH / A2A for group-ticket bookings. */
    private String groupTicketType;
}
