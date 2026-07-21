package com.aerionsoft.application.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AirportDto implements Serializable {
    @JsonProperty("id")
    private Long Id;
    @JsonProperty("Code")
    private String code;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("CityName")
    private String cityName;
    @JsonProperty("CityCode")
    private String cityCode;
    @JsonProperty("CountryCode")
    private String countryCode;
    @JsonProperty("CountryName")
    private String countryName;
    @JsonProperty("Lat")
    private String lat;
    @JsonProperty("Lon")
    private String lon;
    @JsonProperty("Timezone")
    private String timezone;
    @JsonProperty("NumAirports")
    private Integer numAirports;
    @JsonProperty("City")
    private String city;
    @JsonProperty("ActiveSuggestion")
    private Integer activeSuggestion;
}
