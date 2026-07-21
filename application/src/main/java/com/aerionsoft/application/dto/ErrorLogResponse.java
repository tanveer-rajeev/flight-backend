package com.aerionsoft.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogResponse {
    private Long id;
    private String serviceName;
    private String errorCode;
    private String errorMessage;
    private String requestData;
    private String responseData;
    private String statusCode;
    private Long userId;
    private String traceId;
    private String endpoint;
    private String method;
    private String ipAddress;
    private String userAgent;
    private String stackTrace;
    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ErrorStatsResponse {
    private String serviceName;
    private Long errorCount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ErrorLogSummaryResponse {
    private Long totalErrors;
    private Long errorsLast24Hours;
    private java.util.List<ErrorStatsResponse> errorsByService;
    private java.util.List<ErrorStatsResponse> recentErrorsByService;
}
