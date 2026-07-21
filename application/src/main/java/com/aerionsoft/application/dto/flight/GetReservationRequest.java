package com.aerionsoft.application.dto.flight;

import com.aerionsoft.application.enums.booking.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class GetReservationRequest {
    @NotBlank(message = "PNR is required")
    private String pnr;

    @NotNull(message = "Provider is required")
    private Provider provider;

    @Positive(message = "MotherUserID is required")
    private long motherUserId;

    private String channel;

    public void setProvider(Provider provider) {
        this.provider = provider;
    }
}
