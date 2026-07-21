package com.aerionsoft.application.dto.flight.arabiaBaggage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaggageValidationWrapper {
    private boolean success;
    private String message;
    private int status;
    private Response data;
}

