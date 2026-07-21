package com.aerionsoft.application.enums.common;

import org.springframework.http.HttpStatus;

/**
 * Stable machine-readable error codes the frontend can branch on. Each value
 * carries a default HTTP status and a default user-facing message that is used
 * when the throwing site does not provide a more specific one.
 *
 * <p>Adding a new value here is a public API change for the frontend — keep the
 * list small, document each addition in {@code md/error-contract.md}, and never
 * rename existing values.</p>
 */
public enum ErrorCode {

    // Validation / request-shape problems (4xx)
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed. Please check the highlighted fields."),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "Request body is malformed or unreadable."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "A required parameter is missing."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "Uploaded file is invalid."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the maximum allowed limit."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later."),

    // Auth (4xx)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action."),

    // Resource lookup (4xx)
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested item was not found."),

    // Business rule violations (4xx)
    BUSINESS_ERROR(HttpStatus.BAD_REQUEST, "The operation could not be completed."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "Insufficient wallet balance for this operation."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "This record already exists."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Operation conflicts with existing data."),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "The resource is not in a state that allows this operation."),
    PRICE_UNAVAILABLE(HttpStatus.BAD_REQUEST, "Pricing for the selected flight is no longer available."),
    BOOKING_FAILED(HttpStatus.BAD_REQUEST, "Booking could not be completed."),
    TICKET_ISSUE_FAILED(HttpStatus.BAD_REQUEST, "Ticket could not be issued."),
    PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "Payment is required to continue."),

    // External / upstream (4xx-5xx)
    MICROSERVICE_ERROR(HttpStatus.BAD_GATEWAY, "An upstream service returned an error."),
    CORE_BOOKING_ERROR(HttpStatus.BAD_GATEWAY, "Core booking service returned an error."),
    EMAIL_ERROR(HttpStatus.BAD_GATEWAY, "Failed to send email."),
    PAYMENT_ERROR(HttpStatus.BAD_GATEWAY, "Payment processing failed."),

    // Server (5xx)
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred. Please try again later."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getStatus() {
        return httpStatus.value();
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
