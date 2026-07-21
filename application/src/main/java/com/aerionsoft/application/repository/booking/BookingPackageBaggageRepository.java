package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.Booking.BookingPackageBaggage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingPackageBaggageRepository extends JpaRepository<BookingPackageBaggage, Long> {
    List<BookingPackageBaggage> findByBookingId(Long bookingId);
    void deleteByBookingId(Long bookingId);
}
