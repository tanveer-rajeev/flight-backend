package com.aerionsoft.application.dto.notification;

import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private Long businessId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    private NotificationPriority priority;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private String actionUrl;
    private String actionLabel;
    private String referenceId;
    private String referenceType;
    private Map<String, Object> metadata;
    private LocalDateTime expiresAt;
    private Long createdBy;
}

