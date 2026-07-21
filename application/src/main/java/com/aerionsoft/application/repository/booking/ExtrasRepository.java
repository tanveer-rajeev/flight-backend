package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.Booking.Extras;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ExtrasRepository extends JpaRepository<Extras, Long>, JpaSpecificationExecutor<Extras>{
    List<Extras> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);
}
