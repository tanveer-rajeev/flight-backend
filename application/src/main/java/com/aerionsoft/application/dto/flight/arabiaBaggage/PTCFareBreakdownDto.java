package com.aerionsoft.application.dto.flight.arabiaBaggage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PTCFareBreakdownDto {
    @JsonProperty("passengerType")
    private String passengerType;
    // ADT, INF
    @JsonProperty("quantity")
    private int quantity;
    @JsonProperty("baseFare")
    private String baseFare;
    @JsonProperty("totalFare")
    private String totalFare;
    @JsonProperty("taxes")
    private List<TaxDto> taxes;
}