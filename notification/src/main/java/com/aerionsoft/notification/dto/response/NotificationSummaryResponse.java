package com.aerionsoft.notification.dto.response;

import java.util.List;

public record NotificationSummaryResponse(
        long unreadCount,
        List<NotificationResponse> recent
) {
}
