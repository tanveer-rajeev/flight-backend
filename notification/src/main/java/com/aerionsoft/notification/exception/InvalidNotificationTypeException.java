package com.aerionsoft.notification.exception;

public class InvalidNotificationTypeException extends NotificationException {

    public InvalidNotificationTypeException(String typeCode) {
        super("'" + typeCode + "' does not match any registered NotificationType code");
    }
}
