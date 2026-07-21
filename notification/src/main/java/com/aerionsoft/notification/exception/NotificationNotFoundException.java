package com.aerionsoft.notification.exception;

public class NotificationNotFoundException extends NotificationException {

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public static NotificationNotFoundException forId(java.util.UUID id) {
        return new NotificationNotFoundException("Notification not found with id: " + id);
    }
}
