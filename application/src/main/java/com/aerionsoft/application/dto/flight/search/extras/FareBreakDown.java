package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown properties during deserialization

public class FareBreakDown {

    @JsonProperty("Currency")
    @JsonAlias("currency")
    private String Currency;

    @JsonProperty("PassengerType")
    @JsonAlias("passengerType")
    private String PassengerType;

    @JsonProperty("PassengerCount")
    @JsonAlias("passengerCount")
    private int PassengerCount;

    @JsonProperty("BaseFare")
    @JsonAlias("baseFare")
    private double BaseFare;

    @JsonProperty("Tax")
    @JsonAlias("tax")
    private double Tax;


}