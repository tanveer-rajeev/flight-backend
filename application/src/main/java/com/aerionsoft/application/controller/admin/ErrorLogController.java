package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.ErrorLogResponse;
import com.aerionsoft.application.entity.ErrorLog;
import com.aerionsoft.application.service.errorlog.ErrorLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.aerionsoft.application.exception.ResourceNotFoundException;

@RestController
@Validated
@RequestMapping("/api/admin/error-logs")
//@PreAuthorize("hasRole('ADMIN')")
public class ErrorLogController extends BaseController {

    @Autowired
    private ErrorLogService errorLogService;

    @GetMapping
    public ResponseEntity<BaseResponse<Page<ErrorLogResponse>>> getAllErrorLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ErrorLog> errorLogs = errorLogService.getErrorLogs(pageable);

        Page<ErrorLogResponse> response = errorLogs.map(this::mapToResponse);

        return ResponseEntity.ok(BaseResponse.ok("Error logs retrieved successfully", response));
    }

    @GetMapping("/service/{serviceName}")
    public ResponseEntity<BaseResponse<Page<ErrorLogResponse>>> getErrorLogsByService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ErrorLog> errorLogs = errorLogService.getErrorLogsByService(serviceName, pageable);

        Page<ErrorLogResponse> response = errorLogs.map(this::mapToResponse);

        return ResponseEntity.ok(BaseResponse.ok("Error logs for service retrieved successfully", response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<BaseResponse<Page<ErrorLogResponse>>> getErrorLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ErrorLog> errorLogs = errorLogService.getErrorLogsByUser(userId, pageable);

        Page<ErrorLogResponse> response = errorLogs.map(this::mapToResponse);

        return ResponseEntity.ok(BaseResponse.ok("Error logs for user retrieved successfully", response));
    }

    @GetMapping("/trace/{traceId}")
    public ResponseEntity<BaseResponse<Page<ErrorLogResponse>>> getErrorLogsByTraceId(
            @PathVariable String traceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ErrorLog> errorLogs = errorLogService.getErrorLogsByTraceId(traceId, pageable);
        Page<ErrorLogResponse> response = errorLogs.map(this::mapToResponse);

        return ResponseEntity.ok(BaseResponse.ok("Error logs for trace ID retrieved successfully", response));
    }

    @GetMapping("/search")
    public ResponseEntity<BaseResponse<Page<ErrorLogResponse>>> searchErrorLogs(
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ErrorLog> errorLogs = errorLogService.searchErrorLogs(
                traceId, userId, userEmail, serviceName, errorCode, from, to, pageable);
        Page<ErrorLogResponse> response = errorLogs.map(this::mapToResponse);

        return ResponseEntity.ok(BaseResponse.ok("Error logs search completed successfully", response));
    }

    @GetMapping("/stats")
    public ResponseEntity<BaseResponse<Object>> getErrorStats() {
        List<Object[]> statsByService = errorLogService.getErrorStatsByService();
        List<Object[]> recentStats = errorLogService.getRecentErrorsByService();

        var response = new Object() {
            public final List<Object> errorsByService = statsByService.stream()
                    .map(row -> new Object() {
                        public final String serviceName = (String) row[0];
                        public final Long errorCount = (Long) row[1];
                    }).collect(Collectors.toList());

            public final List<Object> recentErrorsByService = recentStats.stream()
                    .map(row -> new Object() {
                        public final String serviceName = (String) row[0];
                        public final Long errorCount = (Long) row[1];
                    }).collect(Collectors.toList());
        };

        return ResponseEntity.ok(BaseResponse.ok("Error statistics retrieved successfully", response));
    }

    @PostMapping("/cleanup")
    public ResponseEntity<BaseResponse<Void>> manualCleanup() {
        errorLogService.manualCleanup();
        return ResponseEntity.ok(BaseResponse.ok("Error logs cleanup completed successfully"));
    }

//    @PostMapping("/export")
//    public ResponseEntity<BaseResponse<String>> exportErrorLogs(
//            @RequestParam(value = "days", defaultValue = "30") int days) {
//        try {
//            LocalDateTime cutoffDate = UserDateTimeUtil.now().minusDays(days);
//            errorLogService.exportOldLogsToZip(cutoffDate);
//
//            String timestamp = UserDateTimeUtil.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
//            String fileName = String.format("error_logs_export_%s.zip", timestamp);
//
//            return ResponseEntity.ok(BaseResponse.ok("Error logs exported successfully",
//                    "Export saved to logs/" + fileName));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(BaseResponse.error("Failed to export error logs: " + e.getMessage()));
//        }
//    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ErrorLogResponse>> getErrorLogById(@PathVariable Long id) {
        ErrorLog errorLog = errorLogService.getErrorLogById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Error log not found with id: " + id));
        return ResponseEntity.ok(BaseResponse.ok("Error log retrieved successfully", mapToResponse(errorLog)));
    }

    private ErrorLogResponse mapToResponse(ErrorLog errorLog) {
        return ErrorLogResponse.builder()
                .id(errorLog.getId())
                .serviceName(errorLog.getServiceName())
                .errorCode(errorLog.getErrorCode())
                .errorMessage(errorLog.getErrorMessage())
                .requestData(errorLog.getRequestData())
                .responseData(errorLog.getResponseData())
                .statusCode(errorLog.getStatusCode())
                .userId(errorLog.getUserId())
                .traceId(errorLog.getTraceId())
                .endpoint(errorLog.getEndpoint())
                .method(errorLog.getMethod())
                .ipAddress(errorLog.getIpAddress())
                .userAgent(errorLog.getUserAgent())
                .stackTrace(errorLog.getStackTrace())
                .createdAt(errorLog.getCreatedAt())
                .build();
    }
}
