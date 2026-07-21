package com.aerionsoft.application.exception;

import lombok.Getter;

@Getter
public class EmailException extends RuntimeException {
    private final String technicalMessage;
    private final EmailErrorType errorType;

    public EmailException(String message, String technicalMessage, EmailErrorType errorType) {
        super(message);
        this.technicalMessage = technicalMessage;
        this.errorType = errorType;
    }

    public EmailException(String message, String technicalMessage, EmailErrorType errorType, Throwable cause) {
        super(message, cause);
        this.technicalMessage = technicalMessage;
        this.errorType = errorType;
    }

    /**
     * Parses the exception message to determine the error type and returns a user-friendly message
     */
    public static EmailException fromMailException(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String technicalMessage = e.getMessage();

        if (message.contains("sender address rejected") || message.contains("not owned by user")) {
            return new EmailException(
                "Unable to send email. Please contact support.",
                technicalMessage,
                EmailErrorType.INVALID_SENDER_ADDRESS,
                e
            );
        } else if (message.contains("invalid address") || message.contains("recipient rejected")
                || message.contains("mailbox not found") || message.contains("user unknown")) {
            return new EmailException(
                "The recipient email address is invalid. Please check and try again.",
                technicalMessage,
                EmailErrorType.INVALID_RECIPIENT_ADDRESS,
                e
            );
        } else if (message.contains("authentication") || message.contains("535") || message.contains("login")) {
            return new EmailException(
                "Email service is temporarily unavailable. Please try again later.",
                technicalMessage,
                EmailErrorType.AUTHENTICATION_FAILED,
                e
            );
        } else if (message.contains("connection") || message.contains("timeout") || message.contains("unreachable")) {
            return new EmailException(
                "Email service is temporarily unavailable. Please try again later.",
                technicalMessage,
                EmailErrorType.CONNECTION_FAILED,
                e
            );
        } else if (message.contains("no active email credentials")) {
            return new EmailException(
                "Email service is not configured. Please contact support.",
                technicalMessage,
                EmailErrorType.NO_CREDENTIALS,
                e
            );
        } else {
            return new EmailException(
                "Failed to send email. Please try again later or contact support.",
                technicalMessage,
                EmailErrorType.GENERAL_ERROR,
                e
            );
        }
    }
}
