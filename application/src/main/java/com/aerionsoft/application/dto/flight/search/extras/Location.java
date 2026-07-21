package com.aerionsoft.application.dto.flight.search.extras;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown properties during deserialization

public class Location {

    @JsonProperty("Airport")
    @JsonAlias("airport")
    private Airport Airport;

    @JsonProperty("DepTime")
    private String DepTime;

    @JsonProperty("ArrTime")
    private String ArrTime;
}

