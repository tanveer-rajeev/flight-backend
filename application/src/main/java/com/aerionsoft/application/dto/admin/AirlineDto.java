package com.aerionsoft.application.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AirlineDto implements Serializable {
    @JsonProperty("AirlineId")
    private Long airlineId;
    @JsonProperty("FS")
    private String FS;
    @JsonProperty("IATA")
    private String IATA;
    @JsonProperty("ICAO")
    private String ICAO;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Active")
    private Integer active;
    @JsonProperty("IsDomestic")
    private Integer isDomestic;
}