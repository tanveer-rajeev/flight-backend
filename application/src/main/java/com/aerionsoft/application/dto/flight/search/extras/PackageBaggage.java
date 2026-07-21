package com.aerionsoft.application.dto.flight.search.extras;

import lombok.Data;

@Data
public class PackageBaggage {
    private String pax;
    private Double weight;
    private String unit;
    private Integer flightNumber;
}
