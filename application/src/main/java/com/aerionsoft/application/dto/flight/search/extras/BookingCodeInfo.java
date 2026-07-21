package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingCodeInfo {
    private String carrier;
    private String flightNumber;
    private String origin;
    private String destination;
    private String departureTime;
    private Integer leg;
    private List<BookingCodeCabin> cabins;
}
