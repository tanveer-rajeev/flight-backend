package com.aerionsoft.application.entity;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notification_templates")

public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code", nullable = false, unique = true, length = 100)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "notification_type")
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, columnDefinition = "notification_priority")
    private NotificationPriority priority;

    @Column(name = "title_template", nullable = false, length = 255)
    private String titleTemplate;

    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;

    @Column(name = "action_url_template", length = 500)
    private String actionUrlTemplate;

    @Column(name = "action_label", length = 100)
    private String actionLabel;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
        if (isActive == null) {
            isActive = true;
        }
        if (priority == null) {
            priority = NotificationPriority.MEDIUM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}

