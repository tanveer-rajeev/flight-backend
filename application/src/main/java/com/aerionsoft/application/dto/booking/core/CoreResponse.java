package com.aerionsoft.application.dto.booking.core;

import com.aerionsoft.application.dto.booking.FlightSSR;
import com.aerionsoft.application.dto.booking.TravelInformation;
import com.aerionsoft.application.enums.booking.BookingStatus;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class CoreResponse {
    public static CoreResponse failure(String message, String reason) {
        CoreResponse response = new CoreResponse();
        response.setStatus(BookingStatus.FAILED);
        response.setMessage(message);
        response.setReason(reason);
        return response;
    }

    private BookingStatus status;
    private String message;
    private String reason;
    private String pnr;
    private String ticketNo;
    private String bookingDate;
    private String airline;
    private String flightNo;
    private Boolean isPriceChanged;
    private String oldPrice;
    private String newPrice;
    private String currency;
    private String airlinePnrs;
    private TravelInformation travelInformation;
    private String transactionIdentifier;
    private Long secondsUntilDeadline;
    private String providerBookingTime;
    private List<PassengerTicketDTO> tickets;
    private String lastPaymentDate;
    private String bookedTimeOffset;

    List<FlightSSR> flightSSRList;
}
