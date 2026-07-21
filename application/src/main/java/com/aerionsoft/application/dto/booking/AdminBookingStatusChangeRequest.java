package com.aerionsoft.application.dto.booking;

import com.aerionsoft.application.enums.booking.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingStatusChangeRequest {

    @NotNull(message = "Booking status is required")
    private BookingStatus status;

    @NotNull(message = "Reason is required")
    private String reason;
}

