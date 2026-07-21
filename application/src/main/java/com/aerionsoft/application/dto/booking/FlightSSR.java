package com.aerionsoft.application.dto.booking;

import lombok.Data;

@Data
public class FlightSSR {
    private String flightNumber;
    private SSRRequest ssrRequest;
}
