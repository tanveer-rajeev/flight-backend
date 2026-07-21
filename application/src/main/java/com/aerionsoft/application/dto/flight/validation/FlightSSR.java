package com.aerionsoft.application.dto.flight.validation;

import com.aerionsoft.application.dto.booking.SSRRequest;
import lombok.Data;

@Data
public class FlightSSR {
    private String flightNumber;
    private SSRRequest ssrRequest;
}
