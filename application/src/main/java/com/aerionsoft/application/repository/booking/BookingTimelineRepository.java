package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.Booking.BookingTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface BookingTimelineRepository extends JpaRepository<BookingTimeline, Long> {

    List<BookingTimeline> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    List<BookingTimeline> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    List<BookingTimeline> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<BookingTimeline> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    void deleteByBookingId(Long bookingId);
}

