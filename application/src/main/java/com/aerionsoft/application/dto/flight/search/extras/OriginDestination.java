package com.aerionsoft.application.dto.flight.search.extras;

import lombok.Data;

@Data
public class OriginDestination {

    private String origin;
    private String destination;
    private String departureDate;
}
