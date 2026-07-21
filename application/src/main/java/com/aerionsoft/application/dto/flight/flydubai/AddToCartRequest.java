package com.aerionsoft.application.dto.flight.flydubai;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddToCartRequest {

    @NotBlank(message = "channel is required")
    private String channel;

    @NotBlank(message = "resultIndex is required")
    private String resultIndex;

    private String outbound;
    private String outboundKey;
    private String inbound;
    private String inboundKey;
}
