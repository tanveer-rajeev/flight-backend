package com.aerionsoft.application.entity;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "email_enabled")
    private Boolean emailEnabled;

    @Column(name = "sms_enabled")
    private Boolean smsEnabled;

    @Column(name = "push_enabled")
    private Boolean pushEnabled;

    @Column(name = "in_app_enabled")
    private Boolean inAppEnabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "type_preferences", columnDefinition = "jsonb")
    private Map<String, Object> typePreferences;

    @Column(name = "quiet_hours_enabled")
    private Boolean quietHoursEnabled;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

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
        if (emailEnabled == null) emailEnabled = true;
        if (smsEnabled == null) smsEnabled = true;
        if (pushEnabled == null) pushEnabled = true;
        if (inAppEnabled == null) inAppEnabled = true;
        if (quietHoursEnabled == null) quietHoursEnabled = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }

    // Helper method to check if notifications should be sent during quiet hours
    public boolean isInQuietHours() {
        if (!Boolean.TRUE.equals(quietHoursEnabled) || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        LocalTime now = LocalTime.now();
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        } else {
            // Quiet hours span midnight
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        }
    }
}

