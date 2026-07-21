package com.aerionsoft.application.entity;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.enums.notification.DeliveryChannel;
import com.aerionsoft.application.enums.notification.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_delivery_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_channel", nullable = false, length = 50)
    private DeliveryChannel deliveryChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeliveryStatus status;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        if (status == null) {
            status = DeliveryStatus.PENDING;
        }
    }

    // Helper methods
    public void markAsSent() {
        this.status = DeliveryStatus.SENT;
        this.sentAt = UserDateTimeUtil.now();
    }

    public void markAsDelivered() {
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = UserDateTimeUtil.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = DeliveryStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}

