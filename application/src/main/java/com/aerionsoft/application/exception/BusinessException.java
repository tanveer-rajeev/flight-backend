package com.aerionsoft.application.exception;

import com.aerionsoft.application.enums.common.ErrorCode;
import lombok.Getter;

import java.util.Map;

/**
 * Thrown by services and controllers when a business rule is violated. The
 * accompanying {@link ErrorCode} drives both the HTTP status and the
 * machine-readable code returned to the frontend; {@link #details} carries
 * structured context the UI may render (e.g. {@code Map.of("required", x,
 * "available", y)} for an insufficient-balance error). Use
 * {@link ResourceNotFoundException} for missing entities and the bean
 * validation framework for shape errors.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object details;
    private final Map<String, String> fields;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = null;
        this.fields = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
        this.fields = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
        this.fields = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, String> fields, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
        this.fields = fields;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
        this.fields = null;
    }

    /** Convenience for a validation-style error originating outside bean validation. */
    public static BusinessException validation(String message, Map<String, String> fields) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message, fields, null);
    }
}
