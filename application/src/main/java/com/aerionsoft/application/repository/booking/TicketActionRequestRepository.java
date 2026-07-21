package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.Booking.TicketActionRequest;
import com.aerionsoft.application.enums.booking.TicketActionStatus;
import com.aerionsoft.application.enums.booking.TicketActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketActionRequestRepository extends JpaRepository<TicketActionRequest, Long> {

    Page<TicketActionRequest> findByBookingId(Long bookingId, Pageable pageable);

    List<TicketActionRequest> findByStatus(TicketActionStatus status);

    Optional<TicketActionRequest> findByIdAndBookingId(Long id, Long bookingId);

    boolean existsByBookingIdAndTypeAndStatusIn(Long bookingId, TicketActionType type, List<TicketActionStatus> statuses);

    Page<TicketActionRequest> findByStatus(TicketActionStatus status, Pageable pageable);

    Page<TicketActionRequest> findByType(TicketActionType type, Pageable pageable);

    Page<TicketActionRequest> findByStatusAndType(TicketActionStatus status, TicketActionType type, Pageable pageable);

    boolean existsByBookingIdAndStatus(Long bookingId, TicketActionStatus status);

    List<TicketActionRequest> findByBookingIdAndStatus(Long bookingId, TicketActionStatus status);
}
