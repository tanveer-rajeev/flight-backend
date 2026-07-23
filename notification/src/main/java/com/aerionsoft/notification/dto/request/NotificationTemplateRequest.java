package com.aerionsoft.notification.dto.request;

import com.aerionsoft.notification.enums.NotificationPriority;

public record NotificationTemplateRequest(
        String typeCode,
        String locale,
        String titleTemplate,
        String messageTemplate,
        NotificationPriority defaultPriority,
        String defaultReferenceType
) {
}