package com.aerionsoft.application.entity.Booking;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "booking_traveller_tickets",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_booking_traveller",
        columnNames = {"booking_id", "traveller_id"}
    )
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class BookingTravellerTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "traveller_id", nullable = false)
    private Long travellerId;

    @Column(name = "ticket_number")
    private String ticketNumber;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;
}

