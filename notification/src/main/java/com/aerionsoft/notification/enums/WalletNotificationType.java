package com.aerionsoft.notification.enums;

import com.aerionsoft.notification.entity.NotificationType;

public enum WalletNotificationType implements NotificationType {
    DEPOSIT_CREATED,
    DEPOSIT_APPROVED,
    DEPOSIT_CANCELLED;

    @Override public String getCode() { return "WALLET_" + name(); }
    @Override public NotificationCategory getCategory() { return NotificationCategory.WALLET; }
}
