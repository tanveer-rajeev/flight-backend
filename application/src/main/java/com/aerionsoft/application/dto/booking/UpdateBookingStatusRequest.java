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
public class UpdateBookingStatusRequest {

    @NotNull(message = "Booking status is required")
    private BookingStatus status;

    private String ticketNumber;

    private String reason;
}

