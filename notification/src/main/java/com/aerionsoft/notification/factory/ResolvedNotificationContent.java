package com.aerionsoft.notification.factory;

import com.aerionsoft.notification.enums.NotificationPriority;

public record ResolvedNotificationContent(
        String title,
        String message,
        NotificationPriority defaultPriority,
        String defaultReferenceType
) {
}
