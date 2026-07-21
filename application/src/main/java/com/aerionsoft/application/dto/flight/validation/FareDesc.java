package com.aerionsoft.application.dto.flight.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FareDesc {
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("fareExchangeRate")
    private Double fareExchangeRate;
}
