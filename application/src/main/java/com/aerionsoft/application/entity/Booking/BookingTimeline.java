package com.aerionsoft.application.entity.Booking;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.converter.BookingStatusAttributeConverter;
import com.aerionsoft.application.enums.booking.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_timeline", indexes = {
        @Index(name = "idx_booking_timeline_booking_id", columnList = "booking_id"),
        @Index(name = "idx_booking_timeline_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "result_index")
    private String resultIndex;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "successful")
    private Boolean successful;

    @Convert(converter = BookingStatusAttributeConverter.class)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Convert(converter = BookingStatusAttributeConverter.class)
    @Column(name = "previous_status")
    private BookingStatus previousStatus;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "pnr")
    private String pnr;

    @Column(name = "ticket_no")
    private String ticketNo;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "actor_type")
    private String actorType; // USER, ADMIN, SYSTEM

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = UserDateTimeUtil.now();
        }
    }
}

