package com.aerionsoft.application.dto.notification;

import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateResponse {
    private Long id;
    private String templateCode;
    private NotificationType type;
    private NotificationPriority priority;
    private String titleTemplate;
    private String messageTemplate;
    private String actionUrlTemplate;
    private String actionLabel;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
