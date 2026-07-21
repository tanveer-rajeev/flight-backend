package com.aerionsoft.application.dto.flight.search;

import lombok.Data;

import java.util.List;

@Data
public class Request {
    private String origin;
    private String destination;
    private String departureDate;
    private String returnDate;
    private int adults = 0;
    private int children = 0;
    private int infants = 0;
    private int cabinClass = 1;
    private boolean manualCombination = false;
    /** Allowed providers for this search. Null means all providers. */
    private List<String> providers;
}
