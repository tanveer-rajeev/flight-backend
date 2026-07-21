package com.aerionsoft.application.dto.flight.arabiaBaggage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FareDto {
    @JsonProperty("baseFare")
    private String baseFare;
    @JsonProperty("totalFare")
    private String totalFare;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("offerFare")
    private String offerFare;


}