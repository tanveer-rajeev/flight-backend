package com.aerionsoft.application.dto.notification;

import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationStatus;
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
public class NotificationFilterRequest {
    private NotificationType type;
    private NotificationStatus status;
    private NotificationPriority priority;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String referenceType;
    private String referenceId;
    private int page = 0;
    private int size = 20;
}

