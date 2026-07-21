package com.aerionsoft.application.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingRequest {

    /**
     * Confirmation ID from core/GDS. In our system this maps to Booking.pnr.
     */
    @NotBlank(message = "Pnr/ConfirmationId must not be blank")
    private String confirmationId;

    @NotBlank(message = "Channel must not be blank")
    private String channel;

    private String reason = "Cancelled by user";

}

