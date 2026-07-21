package com.aerionsoft.application.dto.flight.arabiaBaggage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Response {
    @JsonProperty("transactionIdentifier")
    private String transactionIdentifier;
    @JsonProperty("fare")
    private FareDto fare;
    @JsonProperty("fareBreakdowns")
    private List<PTCFareBreakdownDto> fareBreakdowns;
}

