package com.aerionsoft.notification.email;

public interface EmailClient {
    void sendEmail(String toAddress, String subject, String htmlBody);
}
