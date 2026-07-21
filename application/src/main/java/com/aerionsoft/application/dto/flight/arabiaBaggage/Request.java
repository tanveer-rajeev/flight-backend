package com.aerionsoft.application.dto.flight.arabiaBaggage;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Request {
    @NotBlank(message = "resultIndex is required")
    private String resultIndex;
    @NotBlank(message = "bundleCode is required")
    private String bundleCode;
    @NotBlank(message = "channel is required")
    private String channel;
    @NotBlank(message = "providerName is required")
    private String providerName;
}
