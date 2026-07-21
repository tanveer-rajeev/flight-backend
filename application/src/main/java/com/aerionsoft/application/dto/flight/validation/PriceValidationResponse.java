package com.aerionsoft.application.dto.flight.validation;

import com.aerionsoft.application.enums.booking.BookingStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceValidationResponse {
    private BookingStatus status;
    private String message;
    private String reason;
    private String pnr;
    private String ticketNo;
    private String bookingDate;
    private String airline;
    private String flightNo;
    private Boolean isPriceChanged;
    private String oldPrice;
    private String newPrice;
    private String currency;
    private Object travelInformation;
    private String lastPaymentDate;
    private List<Object> flightSSRList;
    private String bundleCode;

    // Markup-related fields (added by this backend)
    private String originalPrice;  // Price before markup (from core/GDS)
    private String markupAmount;   // Markup amount applied
    private String finalPrice;     // Final price after markup (same as newPrice for display)
}

