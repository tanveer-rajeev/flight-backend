package com.aerionsoft.application.interafces;

import com.aerionsoft.application.dto.booking.BookConformation;
import com.aerionsoft.application.dto.booking.BookingRequest;
import com.aerionsoft.application.dto.booking.BookingResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.booking.BookingStatus;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public interface BookingInterface {

    HashMap<String, Collection<Long>> getTravellerIdsByStatus(Long userId);
    List<BookingResponse> getAllBookingsByUserId(Long userId);
    HashMap<String, Integer> getBookingCountByStatus(Long userId);
    BookingResponse create(BookingRequest req, Long userId, List<Long> travellerIds, BookConformation bookConformation,
                           String price, Long actingUserId, String originalPrice, String buyPrice, String markupAmount,
                           User user, double exchangeRate, String userCurrency);
    Booking getBookingById(Long id);
    void updatePnrAndTicketNo(Long id, String pnr, String ticketNo, BookingStatus status, String reason);

    void updatePnrAndTicketNo(Long id, String pnr, String ticketNo, BookingStatus status, String reason, String airlinePnrs);

    void updateBookingStatus(Long id, BookingStatus status, String reason, String ticketNo);

    void updateBookingStatus(Long id, BookingStatus status, String reason, String ticketNo, boolean isAdmin);

    void updateBookingStatus(Long id, BookingStatus status, String reason, String ticketNo, boolean isAdmin, String airlinePnrs);

    void updateBookingStatus(Long id, BookingStatus status, String reason, String ticketNo, boolean isAdmin, Long adminUserId, String airlinePnrs);

    Booking bookingByPnr(String pnr);

    void updateStatusOnly(Long id, BookingStatus status);
}
