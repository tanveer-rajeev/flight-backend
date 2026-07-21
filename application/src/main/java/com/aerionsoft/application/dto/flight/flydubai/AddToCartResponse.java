package com.aerionsoft.application.dto.flight.flydubai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddToCartResponse {
    private boolean success;
    private String message;
    private JsonNode data;
}
