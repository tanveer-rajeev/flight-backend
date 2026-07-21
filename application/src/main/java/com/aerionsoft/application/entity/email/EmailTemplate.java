package com.aerionsoft.application.entity.email;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "email_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "template_type")
    private String templateType;

    @ElementCollection
    @CollectionTable(name = "email_template_variables", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "variable")
    @Builder.Default
    private List<String> variables = new ArrayList<>();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @Column(name = "business_id", nullable = false)
    @Builder.Default
    private Long businessId = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}
