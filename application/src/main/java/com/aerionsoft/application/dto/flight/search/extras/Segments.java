package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown properties during deserialization

public class Segments {
    private String unit;
    private int leg;
    private String date;
    private String Baggage;
    private String CabinBaggage;
    @JsonAlias("cabinClass")
    private String CabinClass;
    @JsonAlias("cabinClassName")
    private String CabinClassName;
    private String SupplierFareClass;
    @JsonAlias("airline")
    private Airline Airline;
    @JsonAlias("noOfSeatAvailable")
    private Integer NoOfSeatAvailable;
    @JsonAlias("origin")
    private Location Origin;
    @JsonAlias("destination")
    private Location Destination;
    @JsonAlias("duration")
    private Integer Duration;
    private Boolean StopOver;
    private String Craft;
    private String Remark;
    private String rph;
    private String baggagePieceCount;
    private String bookingCode;
}
