package com.aerionsoft.application.dto;

import com.aerionsoft.application.enums.common.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

/**
 * Structured error payload carried inside {@link BaseResponse#getError()}.
 *
 * <p>Frontends should branch on {@link #code} (machine-readable, stable) rather than
 * the human-readable top-level message. {@link #fields} is populated for validation
 * errors so forms can show inline messages keyed by field name. {@link #details}
 * carries domain-specific context (e.g. {@code requiredAmount}, {@code availableBalance})
 * for actionable errors. {@link #traceId} correlates the response to server logs.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError implements Serializable {

    private ErrorCode code;

    private Map<String, String> fields;

    private Object details;

    private String traceId;

    public static ApiError of(ErrorCode code) {
        return ApiError.builder().code(code).build();
    }

    public static ApiError of(ErrorCode code, String traceId) {
        return ApiError.builder().code(code).traceId(traceId).build();
    }

    public static ApiError validation(Map<String, String> fields, String traceId) {
        return ApiError.builder()
                .code(ErrorCode.VALIDATION_ERROR)
                .fields(fields)
                .traceId(traceId)
                .build();
    }

    public static ApiError business(ErrorCode code, Object details, String traceId) {
        return ApiError.builder()
                .code(code)
                .details(details)
                .traceId(traceId)
                .build();
    }
}
