package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.Booking.CoreBookingApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoreBookingApiLogRepository extends JpaRepository<CoreBookingApiLog, Long> {

    List<CoreBookingApiLog> findByTraceIdOrderByCreatedAtDesc(String traceId);

    List<CoreBookingApiLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
