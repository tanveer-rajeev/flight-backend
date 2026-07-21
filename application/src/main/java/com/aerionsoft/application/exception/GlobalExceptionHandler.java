package com.aerionsoft.application.exception;

import com.aerionsoft.application.service.user.CustomUserDetails;
import com.aerionsoft.application.dto.ApiError;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.enums.common.MicroserviceType;
import com.aerionsoft.application.service.errorlog.ErrorCodeMappingService;
import com.aerionsoft.application.service.errorlog.ErrorLogService;
import com.aerionsoft.application.util.StructuredLog;
import com.aerionsoft.application.util.TraceIdSupport;
import com.aerionsoft.notification.exception.NotificationNotFoundException;
import com.stripe.exception.StripeException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired
    private ErrorCodeMappingService errorCodeMappingService;

    @Autowired
    private ErrorLogService errorLogService;

    @ExceptionHandler(TabbyPaymentNotFoundException.class)
    public ProblemDetail handleTabbyPaymentNotFoundException(TabbyPaymentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Tabby payment not found");
        problem.setProperty("tabbyPaymentId", ex.tabbyPaymentId());
        return problem;
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ProblemDetail handleNotificationNotFoundException(TabbyPaymentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Tabby payment not found");
        problem.setProperty("tabbyPaymentId", ex.tabbyPaymentId());
        return problem;
    }

    @ExceptionHandler(TabbyInvalidPaymentStateException.class)
    public ProblemDetail handleInvalidStateException(TabbyInvalidPaymentStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Invalid payment state transition");
        problem.setProperty("tabbyPaymentId", ex.tabbyPaymentId());
        problem.setProperty("actualStatus", ex.actualStatus());
        problem.setProperty("expectedStatus", ex.expectedStatus());
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        StructuredLog.warn(log, "business.exception",
                "traceId", traceId,
                "code", ex.getErrorCode().name(),
                "message", ex.getMessage());

        ApiError apiError = ApiError.builder()
                .code(ex.getErrorCode())
                .fields(ex.getFields())
                .details(ex.getDetails())
                .traceId(traceId)
                .build();

        HttpStatus status = ex.getErrorCode().getHttpStatus();
        return ResponseEntity.status(status)
                .body(BaseResponse.error(ex.getErrorCode(), ex.getMessage(), apiError));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        StructuredLog.warn(log, "resource.not_found",
                "traceId", traceId,
                "message", ex.getMessage());

        ApiError apiError = ApiError.of(ErrorCode.RESOURCE_NOT_FOUND, traceId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), apiError));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        StructuredLog.warn(log, "access.denied", "traceId", traceId, "message", ex.getMessage());

        ApiError apiError = ApiError.of(ErrorCode.ACCESS_DENIED, traceId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(ErrorCode.ACCESS_DENIED, apiError));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Malformed JSON: " + ex.getMessage(), ex);
        StructuredLog.warn(log, "request.malformed_json", "traceId", traceId);

        ApiError apiError = ApiError.of(ErrorCode.MALFORMED_JSON, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.MALFORMED_JSON, apiError));
    }

    @ExceptionHandler(CoreBookingException.class)
    public ResponseEntity<BaseResponse<Void>> handleCoreBookingException(CoreBookingException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        errorLogService.logMicroserviceError(
                MicroserviceType.CORE_BOOKING,
                "CORE_BOOKING_FAILED",
                ex.getCoreResponse().getMessage(),
                null,
                ex.getCoreResponse(),
                "400",
                getCurrentUserId(),
                ex
        );

        String userFriendlyMessage = errorCodeMappingService.mapErrorMessage(
                MicroserviceType.CORE_BOOKING,
                ex.getCoreResponse().getMessage()
        );

        ApiError apiError = ApiError.of(ErrorCode.CORE_BOOKING_ERROR, traceId);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(BaseResponse.error(ErrorCode.CORE_BOOKING_ERROR, userFriendlyMessage, apiError));
    }

    @ExceptionHandler(MicroserviceException.class)
    public ResponseEntity<BaseResponse<Void>> handleMicroserviceException(MicroserviceException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        try {
            errorLogService.logMicroserviceError(
                    ex.getServiceType(),
                    ex.getErrorCode(),
                    ex.getServiceMessage(),
                    null,
                    ex.getResponseData(),
                    "400",
                    getCurrentUserId(),
                    ex
            );
        } catch (Exception logEx) {
            StructuredLog.error(log, "error_log.persist_failed", logEx,
                    "traceId", traceId, "reason", logEx.getMessage());
        }

        String userFriendlyMessage;
        if (!"UNKNOWN".equals(ex.getErrorCode())) {
            userFriendlyMessage = errorCodeMappingService.mapErrorCode(ex.getServiceType(), ex.getErrorCode());
        } else {
            userFriendlyMessage = errorCodeMappingService.mapErrorMessage(ex.getServiceType(), ex.getServiceMessage());
        }

        ApiError apiError = ApiError.of(ErrorCode.MICROSERVICE_ERROR, traceId);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(BaseResponse.error(ErrorCode.MICROSERVICE_ERROR, userFriendlyMessage, apiError));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        String message = resolveDataIntegrityMessage(ex);
        ErrorCode errorCode = message.toLowerCase().contains("already")
                ? ErrorCode.DUPLICATE_RESOURCE
                : ErrorCode.DATA_CONFLICT;

        logApplicationErrorSafely("Data integrity violation: " + message, ex);
        StructuredLog.warn(log, "data.integrity_violation", "traceId", traceId, "message", message);

        ApiError apiError = ApiError.of(errorCode, traceId);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(BaseResponse.error(errorCode, message, apiError));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Entity not found: " + ex.getMessage(), ex);
        StructuredLog.warn(log, "entity.not_found", "traceId", traceId, "message", ex.getMessage());

        ApiError apiError = ApiError.of(ErrorCode.RESOURCE_NOT_FOUND, traceId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(ErrorCode.RESOURCE_NOT_FOUND, apiError));
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<BaseResponse<Void>> handleStripeException(StripeException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Stripe error: " + ex.getMessage(), ex);
        StructuredLog.warn(log, "payment.stripe_error", "traceId", traceId, "message", ex.getMessage());

        ApiError apiError = ApiError.of(ErrorCode.BUSINESS_ERROR, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.BUSINESS_ERROR, ex.getMessage(), apiError));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Illegal argument: " + ex.getMessage(), ex);
        StructuredLog.warn(log, "request.invalid_argument", "traceId", traceId, "message", ex.getMessage());

        ApiError apiError = ApiError.of(ErrorCode.BUSINESS_ERROR, traceId);
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Invalid input provided. Please check your data and try again.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.BUSINESS_ERROR, message, apiError));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        Map<String, String> fields = extractFieldErrors(ex);
        logApplicationErrorSafely("Validation failed: " + fields, ex);
        StructuredLog.warn(log, "request.validation_failed", "traceId", traceId, "fieldCount", fields.size());

        ApiError apiError = ApiError.validation(fields, traceId);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(BaseResponse.error(ErrorCode.VALIDATION_ERROR, apiError));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        Map<String, String> fields = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> normalizePropertyPath(violation),
                        ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        logApplicationErrorSafely("Constraint violation: " + fields, ex);
        StructuredLog.warn(log, "request.constraint_violation", "traceId", traceId, "fieldCount", fields.size());

        ApiError apiError = ApiError.validation(fields, traceId);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(BaseResponse.error(ErrorCode.VALIDATION_ERROR, apiError));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Type mismatch: " + ex.getMessage(), ex);
        StructuredLog.warn(log, "request.type_mismatch", "traceId", traceId, "parameter", ex.getName());

        ApiError apiError = ApiError.of(ErrorCode.MISSING_PARAMETER, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.MISSING_PARAMETER,
                        "Invalid parameter format. Please check your request.", apiError));
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<BaseResponse<Void>> handleSQLException(SQLException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("SQL exception: " + ex.getMessage(), ex);
        StructuredLog.error(log, "database.error", ex, "traceId", traceId);

        ApiError apiError = ApiError.of(ErrorCode.DATABASE_ERROR, traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.DATABASE_ERROR, apiError));
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<BaseResponse<Void>> handleEmailException(EmailException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Email exception [" + ex.getErrorType() + "]: " + ex.getTechnicalMessage(), ex);
        StructuredLog.error(log, "email.error", ex, "traceId", traceId, "errorType", ex.getErrorType());

        ApiError apiError = ApiError.of(ErrorCode.EMAIL_ERROR, traceId);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(BaseResponse.error(ErrorCode.EMAIL_ERROR, ex.getMessage(), apiError));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<BaseResponse<Void>> handleMailException(MailException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Mail exception: " + ex.getMessage(), ex);
        StructuredLog.error(log, "email.mail_exception", ex, "traceId", traceId);

        EmailException emailEx = EmailException.fromMailException(ex);
        ApiError apiError = ApiError.of(ErrorCode.EMAIL_ERROR, traceId);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(BaseResponse.error(ErrorCode.EMAIL_ERROR, emailEx.getMessage(), apiError));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<BaseResponse<Void>> handleMultipartException(MultipartException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Multipart exception: " + ex.getMessage(), ex);
        StructuredLog.error(log, "upload.multipart_error", ex, "traceId", traceId);

        String message = resolveMultipartMessage(ex);
        ApiError apiError = ApiError.of(ErrorCode.INVALID_FILE, traceId);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(BaseResponse.error(ErrorCode.INVALID_FILE, message, apiError));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Max upload size exceeded: " + ex.getMessage(), ex);
        StructuredLog.warn(log, "upload.file_too_large", "traceId", traceId);

        ApiError apiError = ApiError.of(ErrorCode.FILE_TOO_LARGE, traceId);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(BaseResponse.error(ErrorCode.FILE_TOO_LARGE, apiError));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<Void>> handleRuntimeException(RuntimeException ex) {
        String traceId = TraceIdSupport.currentTraceId();
        ExceptionClassification.Result classified = ExceptionClassification.classify(ex);

        if (classified.internal()) {
            logApplicationErrorSafely("Runtime exception: " + ex.getMessage(), ex);
            StructuredLog.error(log, "application.runtime_exception", ex, "traceId", traceId);
        } else {
            StructuredLog.warn(log, "application.runtime_exception.business",
                    "traceId", traceId,
                    "code", classified.code().name(),
                    "message", classified.clientMessage());
        }

        ApiError apiError = ApiError.of(classified.code(), traceId);
        return ResponseEntity.status(classified.code().getHttpStatus())
                .body(BaseResponse.error(classified.code(), classified.clientMessage(), apiError));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGenericException(Exception ex) {
        String traceId = TraceIdSupport.currentTraceId();
        logApplicationErrorSafely("Generic exception: " + ex.getMessage(), ex);
        StructuredLog.critical(log, "application.unexpected_exception", ex, "traceId", traceId);

        ApiError apiError = ApiError.of(ErrorCode.INTERNAL_ERROR, traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.INTERNAL_ERROR, apiError));
    }

    private void logApplicationErrorSafely(String errorMessage, Exception ex) {
        try {
            errorLogService.logApplicationError(errorMessage, null, getCurrentUserId(), ex);
        } catch (Exception logEx) {
            StructuredLog.error(log, "error_log.persist_failed", logEx,
                    "reason", logEx.getMessage());
        }
    }

    private Map<String, String> extractFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Invalid value",
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }

    private String normalizePropertyPath(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        String message = "Invalid data provided. Please check your input and try again.";
        String errorMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (errorMessage.contains("foreign key constraint")) {
            if (errorMessage.contains("form_id")) {
                return "The specified form does not exist. Please select a valid form.";
            }
            if (errorMessage.contains("visa_id")) {
                return "The specified visa info does not exist. Please select a valid visa.";
            }
            if (errorMessage.contains("user_id")) {
                return "The specified user does not exist.";
            }
            return "Referenced data not found. Please verify your input.";
        }
        if (errorMessage.contains("unique constraint") || errorMessage.contains("duplicate")) {
            if (errorMessage.contains("email")) {
                return "This email is already in use. Please use a different email.";
            }
            if (errorMessage.contains("passport")) {
                return "This passport number is already registered.";
            }
            return "This data already exists. Please use different values.";
        }
        if (errorMessage.contains("not null constraint")) {
            return "Required fields are missing. Please fill in all required information.";
        }
        return message;
    }

    private String resolveMultipartMessage(MultipartException ex) {
        String message = "File upload failed. ";
        if (ex.getMessage() != null) {
            String errorMsg = ex.getMessage().toLowerCase();
            if (errorMsg.contains("filecountlimitexceeded") || errorMsg.contains("attachment")) {
                return message + "Too many files or form parameters in the request. Please check for duplicate parameter names in your request.";
            }
            if (ex.getMessage().contains("FileSizeLimitExceededException")) {
                return message + "One or more files exceed the maximum allowed size of 20MB.";
            }
            if (ex.getMessage().contains("SizeLimitExceededException")) {
                return message + "The total request size exceeds the maximum allowed size of 20MB.";
            }
        }
        return message + "Please ensure your request has no duplicate parameter names and all files are valid and within size limits.";
    }

    public Long getCurrentUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }
        return null;
    }
}
