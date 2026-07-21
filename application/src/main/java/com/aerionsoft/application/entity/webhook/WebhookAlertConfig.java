package com.aerionsoft.application.entity.webhook;

import com.aerionsoft.application.enums.webhook.WebhookAlertType;
import com.aerionsoft.application.enums.webhook.WebhookChannel;
import com.aerionsoft.application.util.UserDateTimeUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_alert_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookAlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 100)
    private WebhookAlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WebhookChannel channel;

    @Column(name = "webhook_url", nullable = false, columnDefinition = "TEXT")
    private String webhookUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}
