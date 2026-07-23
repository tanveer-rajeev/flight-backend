package com.aerionsoft.notification.dto.request;

import com.aerionsoft.notification.enums.NotificationChannelType;

public record NotificationPreferenceRequest(
        String typeCode,
        NotificationChannelType channel,
        boolean enabled
) {
}
