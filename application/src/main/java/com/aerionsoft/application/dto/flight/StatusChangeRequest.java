package com.aerionsoft.application.dto.flight;

import com.aerionsoft.application.enums.booking.BookingStatus;
import lombok.Data;

@Data
public class StatusChangeRequest {

    public BookingStatus bookingStatus;
    public String reason;
}
