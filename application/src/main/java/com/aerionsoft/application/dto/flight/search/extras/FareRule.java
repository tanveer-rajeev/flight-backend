package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown properties during deserialization

public class FareRule {
    @JsonProperty("Origin")
    private String Origin;

    @JsonProperty("Destination")
    private String Destination;

    @JsonProperty("Airline")
    private String Airline;

    @JsonProperty("FareBasisCode")
    private String FareBasisCode;

    @JsonProperty("FareRuleDetail")
    private String FareRuleDetail;

    @JsonProperty("FareRestriction")
    private String FareRestriction;

    @JsonProperty("FareFamilyCode")
    private String FareFamilyCode;

    @JsonProperty("FareRuleIndex")
    private String FareRuleIndex;
}
