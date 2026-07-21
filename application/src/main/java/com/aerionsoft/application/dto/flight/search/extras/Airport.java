package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown properties during deserialization

public class Airport {
    @JsonProperty("AirportCode")
    @JsonAlias("airportCode")
    private String AirportCode;

    @JsonProperty("AirportName")
    @JsonAlias("airportName")
    private String AirportName;

    @JsonProperty("Terminal")
    private String Terminal;

    @JsonProperty("CityCode")
    private String CityCode;

    @JsonProperty("CityName")
    private String CityName;

    @JsonProperty("CountryCode")
    private String CountryCode;

    @JsonProperty("CountryName")
    private String CountryName;

    @JsonProperty("TimeZoneInfoDetails")
    private String TimeZoneInfoDetails;

    @JsonProperty("UtcOffSet")
    private String UtcOffSet;
}