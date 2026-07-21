package com.aerionsoft.application.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSummaryDTO {
    private Long totalNotifications;
    private Long unreadCount;
    private Long readCount;
    private Long archivedCount;
    private Long urgentCount;
    private Long highPriorityCount;
}

