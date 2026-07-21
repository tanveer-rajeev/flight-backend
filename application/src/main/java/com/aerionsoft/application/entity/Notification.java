package com.aerionsoft.application.entity;

import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationStatus;
import com.aerionsoft.application.enums.notification.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.aerionsoft.application.entity.listener.UserTimestampListener;
import com.aerionsoft.application.util.UserDateTimeUtil;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notifications")
@EntityListeners(UserTimestampListener.class)
public class Notification implements HasCreatedUserTimestamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "business_id")
    private Long businessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "notification_type")
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, columnDefinition = "notification_priority")
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "notification_status")
    private NotificationStatus status;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "action_label", length = 100)
    private String actionLabel;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = UserDateTimeUtil.now();
        }
        if (createdTimeOffset == null) {
            createdTimeOffset = UserDateTimeUtil.currentOffset();
        }
        if (status == null) {
            status = NotificationStatus.UNREAD;
        }
        if (priority == null) {
            priority = NotificationPriority.MEDIUM;
        }
        if (type == null) {
            type = NotificationType.GENERAL;
        }
    }

    public void markAsRead() {
        this.status = NotificationStatus.READ;
        this.readAt = UserDateTimeUtil.now();
    }

    public void markAsArchived() {
        this.status = NotificationStatus.ARCHIVED;
        this.archivedAt = UserDateTimeUtil.now();
    }

    public boolean isUnread() {
        return NotificationStatus.UNREAD.equals(this.status);
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(UserDateTimeUtil.now());
    }
}

