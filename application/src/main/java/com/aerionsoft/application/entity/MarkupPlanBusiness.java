package com.aerionsoft.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "markup_plan_business",
       uniqueConstraints = @UniqueConstraint(columnNames = {"markup_plan_id", "business_id"}))
public class MarkupPlanBusiness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "markup_plan_id", nullable = false)
    private MarkupPlan markupPlan;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;
}

