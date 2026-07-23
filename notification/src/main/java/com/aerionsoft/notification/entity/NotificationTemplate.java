package com.aerionsoft.notification.entity;

import com.aerionsoft.notification.enums.NotificationPriority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"type_code", "locale"})
)
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code", nullable = false, length = 60)
    private String typeCode;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "title_template", nullable = false, length = 255)
    private String titleTemplate;

    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_priority", nullable = false, length = 20)
    private NotificationPriority defaultPriority;

    @Column(name = "default_reference_type", length = 50)
    private String defaultReferenceType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
