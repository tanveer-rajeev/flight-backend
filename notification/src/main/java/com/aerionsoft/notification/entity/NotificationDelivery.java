package com.aerionsoft.notification.entity;

import com.aerionsoft.notification.enums.NotificationChannelType;
import com.aerionsoft.notification.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_deliveries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDelivery{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannelType channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    private LocalDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static NotificationDelivery pendingFor(NotificationChannelType channel) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.channel = channel;
        delivery.status = NotificationStatus.PENDING;
        return delivery;
    }

    void assignTo(Notification notification) {
        this.notification = notification;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
}
