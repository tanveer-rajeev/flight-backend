package com.aerionsoft.application.exception;

import com.aerionsoft.application.enums.common.ErrorCode;

/**
 * Factory helpers for typed service-layer exceptions. Prefer these over raw
 * {@link RuntimeException} so {@link GlobalExceptionHandler} returns the
 * correct HTTP status and error code.
 */
public final class ServiceExceptions {

    private ServiceExceptions() {
    }

    public static ResourceNotFoundException notFound(String resource, Object identifier) {
        return new ResourceNotFoundException(resource, identifier);
    }

    public static ResourceNotFoundException notFound(String message) {
        return new ResourceNotFoundException(message);
    }

    public static BusinessException business(String message) {
        return new BusinessException(ErrorCode.BUSINESS_ERROR, message);
    }

    public static BusinessException duplicate(String message) {
        return new BusinessException(ErrorCode.DUPLICATE_RESOURCE, message);
    }

    public static BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.INVALID_STATE, message);
    }

    public static BusinessException accessDenied(String message) {
        return new BusinessException(ErrorCode.ACCESS_DENIED, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }

    public static BusinessException insufficientBalance(String message) {
        return new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, message);
    }

    public static BusinessException insufficientBalance(String message, Object details) {
        return new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, message, details);
    }

    public static BusinessException payment(String message) {
        return new BusinessException(ErrorCode.PAYMENT_ERROR, message);
    }

    public static BusinessException payment(String message, Throwable cause) {
        return new BusinessException(ErrorCode.PAYMENT_ERROR, message, cause);
    }

    public static BusinessException bookingFailed(String message) {
        return new BusinessException(ErrorCode.BOOKING_FAILED, message);
    }

    public static BusinessException bookingFailed(String message, Throwable cause) {
        return new BusinessException(ErrorCode.BOOKING_FAILED, message, cause);
    }

    public static BusinessException ticketIssueFailed(String message) {
        return new BusinessException(ErrorCode.TICKET_ISSUE_FAILED, message);
    }

    public static BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }

    public static BusinessException fileError(String message) {
        return new BusinessException(ErrorCode.INVALID_FILE, message);
    }

    public static BusinessException fileError(String message, Throwable cause) {
        return new BusinessException(ErrorCode.INVALID_FILE, message, cause);
    }

    public static BusinessException emailError(String message) {
        return new BusinessException(ErrorCode.EMAIL_ERROR, message);
    }

    public static BusinessException microservice(String message) {
        return new BusinessException(ErrorCode.MICROSERVICE_ERROR, message);
    }

    public static BusinessException microservice(String message, Throwable cause) {
        return new BusinessException(ErrorCode.MICROSERVICE_ERROR, message, cause);
    }

    public static BusinessException internal(String message, Throwable cause) {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
