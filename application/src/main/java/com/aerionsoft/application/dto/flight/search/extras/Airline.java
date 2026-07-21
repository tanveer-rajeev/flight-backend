package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown properties during deserialization

public class Airline {
    @JsonProperty("AirlineCode")
    private String AirlineCode;

    @JsonProperty("AirlineName")
    private String AirlineName;

    @JsonProperty("FlightNumber")
    private String FlightNumber;

    @JsonProperty("FareClass")
    private String FareClass;

    @JsonProperty("OperatingCarrier")
    private String OperatingCarrier;
}
