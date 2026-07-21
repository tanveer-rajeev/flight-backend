package com.aerionsoft.application.dto.booking;

import com.aerionsoft.application.enums.booking.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingTimelineDTO {

    private Long id;
    private Long bookingId;
    private String sessionId;
    private String resultIndex;
    private String providerName;
    private Boolean successful;
    private BookingStatus status;
    private BookingStatus previousStatus;
    private String title;
    private String description;
    private String reason;
    private String pnr;
    private String ticketNo;
    private Long actorId;
    private String actorName;
    private String actorType; // USER, ADMIN, SYSTEM
    private LocalDateTime createdAt;
}

