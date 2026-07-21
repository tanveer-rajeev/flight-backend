package com.aerionsoft.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "markup_logs")
public class MarkupLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;
    private String ruleApplied; // Description or ID
    private Double originalPrice;
    private Double markupAmount;
    private Double finalPrice;

    // User context fields
    private String userType; // GUEST, USER, AGENT
    private Long userId;
    private Long businessId;

    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;
}

