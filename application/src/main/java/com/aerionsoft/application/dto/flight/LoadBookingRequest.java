package com.aerionsoft.application.dto.flight;

import com.aerionsoft.application.enums.booking.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoadBookingRequest {

    @NotNull(message = "Provider is required")
    private Provider provider;

    @NotBlank(message = "Date is required")
    private String date;
}
