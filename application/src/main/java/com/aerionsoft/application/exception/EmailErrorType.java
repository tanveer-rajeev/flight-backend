package com.aerionsoft.application.exception;

public enum EmailErrorType {
    INVALID_SENDER_ADDRESS,
    INVALID_RECIPIENT_ADDRESS,
    AUTHENTICATION_FAILED,
    CONNECTION_FAILED,
    SMTP_ERROR,
    NO_CREDENTIALS,
    GENERAL_ERROR
}
