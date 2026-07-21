package com.aerionsoft.application.exception;

import com.aerionsoft.application.enums.common.ErrorCode;

/**
 * Maps legacy {@link RuntimeException} throws to structured error codes until
 * services are fully migrated to {@link BusinessException} and
 * {@link ResourceNotFoundException}.
 */
public final class ExceptionClassification {

    private ExceptionClassification() {
    }

    public record Result(ErrorCode code, String clientMessage, boolean internal) {
    }

    public static Result classify(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return internal();
        }

        String lower = message.toLowerCase();

        if (containsAny(lower, "not found", "does not exist", "no such")) {
            return new Result(ErrorCode.RESOURCE_NOT_FOUND, message, false);
        }
        if (containsAny(lower, "already exists", "already in use", "already associated",
                "duplicate", "already registered")) {
            return new Result(ErrorCode.DUPLICATE_RESOURCE, message, false);
        }
        if (containsAny(lower, "insufficient balance", "insufficient wallet", "insufficient credit")) {
            return new Result(ErrorCode.INSUFFICIENT_BALANCE, message, false);
        }
        if (containsAny(lower, "access denied", "not authorized", "unauthorized", "permission denied")) {
            return new Result(ErrorCode.ACCESS_DENIED, message, false);
        }
        if (containsAny(lower, "invalid state", "not in a state")) {
            return new Result(ErrorCode.INVALID_STATE, message, false);
        }
        if (containsAny(lower, "payment failed", "payment is required", "payment not found")) {
            return new Result(ErrorCode.PAYMENT_ERROR, message, false);
        }
        if (containsAny(lower, "file too large", "failed to upload", "invalid file")) {
            return new Result(ErrorCode.INVALID_FILE, message, false);
        }
        if (containsAny(lower, "validation failed", "required field", "cannot be empty",
                "must be", "must not", "is required", "missing", "invalid")) {
            return new Result(ErrorCode.BUSINESS_ERROR, message, false);
        }
        if (containsTechnicalDetail(lower, message)) {
            return internal();
        }
        if (message.length() <= 250) {
            return new Result(ErrorCode.BUSINESS_ERROR, message, false);
        }
        return internal();
    }

    private static Result internal() {
        return new Result(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), true);
    }

    private static boolean containsAny(String lower, String... terms) {
        for (String term : terms) {
            if (lower.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTechnicalDetail(String lower, String original) {
        return containsAny(lower,
                "sql", "jdbc", "hibernate", "nullpointer", "stacktrace", "exception at",
                "org.", "com.example", "failed to initialize", "connection refused",
                "timeout", "socket", "json parse", "unexpected token")
                || original.contains("Exception:")
                || original.contains(" at ");
    }
}
