package com.aerionsoft.application.dto.flight.farerules;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FareRulesRequest {
    private String resultIndex;
    private String bundleCode;
    private String channel;
}
