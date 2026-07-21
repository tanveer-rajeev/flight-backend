package com.aerionsoft.application.dto.flight.search;

import com.aerionsoft.application.dto.flight.search.extras.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
    private String type, resultIndex, traceId, airlineRemark, resultFareType, channel;
    private String sabre, brandName, colorCode;
    private String status, message, reason, baggage;
    private Boolean isBookableIfSeatNotAvailable, isFreeMealAvailable,
            isHoldAllowedWithSSR, isHoldMandatoryWithSSR, isLCC, isRefundable,
            isPassportRequiredAtBook, isPassportRequiredAtTicket, isReturn, isBookingAllowed,
            isTicketingAllowed, isBundleValidationRequired = false, isPriceChanged;
    private List<FareRule> fareRules;

    private FinalFare fare;

    private List<Segments> segments;

    private List<FareBreakDown> fareBreakDowns;

    private OriginDestination originDestination;


    private FarePackage[] farePackages;

    private List<BookingCodeInfo> bookingCodeInfo;

    public Boolean isAirArabia() {
        return "AIR_ARABIA".equalsIgnoreCase(type);
    }
}
