package com.aerionsoft.application.dto.booking.core;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
public class CoreCancelBookRequest {

    @NotBlank(message = "Pnr/ConfirmationId must not be blank")
    private String confirmationId;

    @NotBlank(message = "Channel must not be blank")
    private String channel;

}
