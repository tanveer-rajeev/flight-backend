package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.Booking.BookingTravellerTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface BookingTravellerTicketRepository extends JpaRepository<BookingTravellerTicket, Long> {

    List<BookingTravellerTicket> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);

    Optional<BookingTravellerTicket> findByBookingIdAndTravellerId(Long bookingId, Long travellerId);

    default Map<Long, String> getTicketMapForBooking(Long bookingId) {
        return findByBookingId(bookingId).stream()
                .filter(t -> t.getTicketNumber() != null)
                .collect(Collectors.toMap(
                        BookingTravellerTicket::getTravellerId,
                        BookingTravellerTicket::getTicketNumber,
                        (a, b) -> a
                ));
    }
}

