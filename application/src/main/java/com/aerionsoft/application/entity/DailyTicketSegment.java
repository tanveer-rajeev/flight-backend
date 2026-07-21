package com.aerionsoft.application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "daily_ticket_segment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pnr", "date", "channel"}))
public class DailyTicketSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String pnr;

    @Column(name = "segment_count", nullable = false)
    private Integer segmentCount;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;
}

