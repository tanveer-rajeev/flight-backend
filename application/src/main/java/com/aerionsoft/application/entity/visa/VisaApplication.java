package com.aerionsoft.application.entity.visa;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.cms.CustomForm;
import com.aerionsoft.application.enums.tour.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "visa_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisaApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(name = "visa_id", nullable = false)
    private Long visaId;

    // Store form responses as JSON structure
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_responses", columnDefinition = "jsonb")
    private Map<String, Object> formResponses;

    // Application Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = UserDateTimeUtil.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private String processedBy;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = UserDateTimeUtil.now();

    @Column(name = "created_by", updatable = true)
    private String createdBy;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", insertable = false, updatable = false)
    private CustomForm customForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visa_id", insertable = false, updatable = false)
    private VisaInfo visaInfo;

    @PreUpdate
    public void preUpdate() {
        if (status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED) {
            this.processedAt = UserDateTimeUtil.now();
        }
    }
}
