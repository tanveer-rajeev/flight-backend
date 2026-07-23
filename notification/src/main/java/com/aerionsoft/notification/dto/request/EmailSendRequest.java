package com.aerionsoft.notification.dto.request;

public record EmailSendRequest(String toAddress,
                               String subject,
                               String htmlBody) {
}
