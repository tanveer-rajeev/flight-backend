package com.aerionsoft.application.dto.flight.search.v1;

import lombok.Data;

@Data
public class OriginDestination {
    private String origin;
    private String destination;
    private String departureDate;
    private String returnDate;
}

