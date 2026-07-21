package com.aerionsoft.application.dto.booking;

import com.aerionsoft.application.enums.booking.BookingStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class BookConformation {
    private String pnr;
    private String ticketNo;
    private String airline;
    private BookingStatus status;
    private OffsetDateTime bookingDate;
    private LocalDateTime updatedAt;
    private String reason;
    private List<FlightSSR> flightDetails;
    private String channel;
    private String airlinePnrs;
    private Long lastPaymentDateInSeconds;
    private String providerBookingTime;
    private String bookedTimeOffset;
    private String sourceType;


    private String lastPaymentDate;


    public BookConformation(String pnr, String ticketNo, String airline,
                            BookingStatus status, OffsetDateTime bookingDate, LocalDateTime updatedAt
            , String reason) {
        this.pnr = pnr;
        this.ticketNo = ticketNo;
        this.airline = airline;
        this.status = status;
        this.bookingDate = bookingDate;
        this.updatedAt = updatedAt;
        this.reason = reason;
    }
}
