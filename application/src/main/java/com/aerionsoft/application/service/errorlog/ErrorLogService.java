package com.aerionsoft.application.service.errorlog;

import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.ErrorLog;
import com.aerionsoft.application.enums.common.MicroserviceType;
import com.aerionsoft.application.repository.common.ErrorLogRepository;
import com.aerionsoft.application.repository.common.ErrorLogSpec;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.util.TraceIdSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class ErrorLogService {

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @Autowired
    private ErrorLogPartitionService errorLogPartitionService;

    @Autowired
    private ErrorLogPersistenceService errorLogPersistenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    @Autowired
    @Qualifier("errorLogExecutor")
    private Executor errorLogExecutor;

    @Value("${error.log.retention-days:30}")
    private int retentionDays;

    @Value("${error.log.cleanup.batch-size:5000}")
    private int cleanupBatchSize;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Log error asynchronously to avoid blocking the main request.
     * traceId and request context are captured on the caller thread before async handoff.
     */
    public void logError(String serviceName, String errorCode, String errorMessage,
                         Object requestData, Object responseData, String statusCode,
                         Long userId, Exception exception) {
        enqueueErrorLog(serviceName, errorCode, errorMessage, requestData, responseData,
                statusCode, userId, exception);
    }

    private void enqueueErrorLog(
            String serviceName,
            String errorCode,
            String errorMessage,
            Object requestData,
            Object responseData,
            String statusCode,
            Long userId,
            Exception exception) {
        String traceId = TraceIdSupport.currentTraceId();
        RequestContextData contextData = captureRequestContext();
        Runnable persistTask = () -> persistErrorLog(
                serviceName, errorCode, errorMessage, requestData, responseData,
                statusCode, userId, exception, traceId, contextData);

        try {
            errorLogExecutor.execute(persistTask);
        } catch (RejectedExecutionException rejected) {
            log.warn("Error log executor saturated; persisting synchronously. traceId={}", traceId, rejected);
            persistTask.run();
        }
    }

    private void persistErrorLog(
            String serviceName,
            String errorCode,
            String errorMessage,
            Object requestData,
            Object responseData,
            String statusCode,
            Long userId,
            Exception exception,
            String traceId,
            RequestContextData contextData) {
        try {
            LocalDateTime createdAt = UserDateTimeUtil.now();

            ErrorLog errorLog = ErrorLog.builder()
                    .createdAt(createdAt)
                    .serviceName(serviceName)
                    .errorCode(errorCode)
                    .errorMessage(truncateIfNeeded(errorMessage, 10000))
                    .requestData(convertToJsonSafely(requestData, 5000))
                    .responseData(convertToJsonSafely(responseData, 5000))
                    .statusCode(statusCode)
                    .userId(userId)
                    .traceId(traceId)
                    .stackTrace(exception != null ? getStackTraceSafely(exception, 20000) : null)
                    .endpoint(contextData.endpoint)
                    .method(contextData.method)
                    .ipAddress(contextData.ipAddress)
                    .userAgent(truncateIfNeeded(contextData.userAgent, 1000))
                    .build();

            long savedId = errorLogPersistenceService.insert(errorLog);
            log.debug("Error logged successfully for service: {}, traceId: {}, id: {}", serviceName, traceId, savedId);

        } catch (Exception e) {
            log.error("Failed to log error to database. service={}, traceId={}", serviceName, traceId, e);
        }
    }

    /**
     * Log microservice error - fire and forget
     */
    public void logMicroserviceError(MicroserviceType serviceType, String errorCode,
                                   String errorMessage, Object requestData, Object responseData,
                                   String statusCode, Long userId, Exception exception) {
        enqueueErrorLog(serviceType.getServiceName(), errorCode, errorMessage,
                requestData, responseData, statusCode, userId, exception);
    }

    /**
     * Log general application error - fire and forget
     */
    public void logApplicationError(String errorMessage, Object requestData,
                                  Long userId, Exception exception) {
        enqueueErrorLog("APPLICATION", "GENERAL_ERROR", errorMessage,
                requestData, null, "500", userId, exception);
    }

    /**
     * Get error logs with pagination
     */
    public Page<ErrorLog> getErrorLogs(Pageable pageable) {
        return errorLogRepository.findAllByOrderByIdDesc(pageable);
    }

    /**
     * Get error logs by service
     */
    public Page<ErrorLog> getErrorLogsByService(String serviceName, Pageable pageable) {
        return errorLogRepository.findByServiceNameOrderByCreatedAtDesc(serviceName, pageable);
    }

    /**
     * Get error logs by user
     */
    public Page<ErrorLog> getErrorLogsByUser(Long userId, Pageable pageable) {
        return errorLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<ErrorLog> getErrorLogsByTraceId(String traceId, Pageable pageable) {
        return errorLogRepository.findByTraceIdOrderByCreatedAtDesc(traceId, pageable);
    }

    public Optional<ErrorLog> getErrorLogById(Long id) {
        return errorLogRepository.findById(id);
    }

    public Page<ErrorLog> searchErrorLogs(
            String traceId,
            Long userId,
            String userEmail,
            String serviceName,
            String errorCode,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        Long resolvedUserId = userId;
        if (resolvedUserId == null && userEmail != null && !userEmail.isBlank()) {
            resolvedUserId = userRepository.findByEmail(userEmail.trim().toLowerCase())
                    .map(user -> user.getId())
                    .orElse(-1L);
            if (resolvedUserId == -1L) {
                return Page.empty(pageable);
            }
        }

        return errorLogRepository.findAll(
                Specification.where(ErrorLogSpec.hasTraceId(normalize(traceId)))
                        .and(ErrorLogSpec.hasUserId(resolvedUserId))
                        .and(ErrorLogSpec.hasServiceName(normalize(serviceName)))
                        .and(ErrorLogSpec.hasErrorCode(normalize(errorCode)))
                        .and(ErrorLogSpec.createdBetween(fromDate, toDate)),
                pageable);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Get error statistics by service
     */
    public List<Object[]> getErrorStatsByService() {
        return errorLogRepository.countErrorsByService();
    }

    /**
     * Get recent errors (last 24 hours) by service
     */
    public List<Object[]> getRecentErrorsByService() {
        LocalDateTime since = UserDateTimeUtil.now().minusHours(24);
        return errorLogRepository.countRecentErrorsByService(since);
    }

    /**
     * Scheduled retention job: export, drop expired partitions, then batched delete
     * for rows still held in the default partition.
     */
    @Scheduled(cron = "${error.log.cleanup.cron:0 0 2 * * SUN}")
    public void cleanupOldErrorLogs() {
        try {
            LocalDateTime cutoffDate = UserDateTimeUtil.now().minusDays(retentionDays);
            exportOldLogsToZip(cutoffDate);

            int droppedPartitions = errorLogPartitionService.dropExpiredPartitions(cutoffDate);
            int deletedRows = deleteOldLogsInBatches(cutoffDate);

            log.info("Error log retention completed: retentionDays={}, droppedPartitions={}, deletedRows={}",
                    retentionDays, droppedPartitions, deletedRows);
        } catch (Exception e) {
            log.error("Failed to cleanup old error logs", e);
        }
    }

    private int deleteOldLogsInBatches(LocalDateTime cutoffDate) {
        int totalDeleted = 0;
        int deletedInBatch;

        do {
            deletedInBatch = errorLogRepository.deleteOldErrorLogsBatch(cutoffDate, cleanupBatchSize);
            totalDeleted += deletedInBatch;
        } while (deletedInBatch == cleanupBatchSize);

        return totalDeleted;
    }

    /**
     * Manual cleanup method with export
     */
    public void manualCleanup() {
        cleanupOldErrorLogs();
    }

    /**
     * Export error logs older than cutoff date to CSV and ZIP
     */
    public void exportOldLogsToZip(LocalDateTime cutoffDate) {
        try {
            // Get logs to be deleted
            List<ErrorLog> oldLogs = errorLogRepository.findByCreatedAtBefore(cutoffDate);

            if (oldLogs.isEmpty()) {
                log.info("No old error logs to export");
                return;
            }

            String timestamp = UserDateTimeUtil.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String csvFileName = String.format("error_logs_export_%s.csv", timestamp);
            String zipFileName = String.format("error_logs_export_%s.zip", timestamp);

            // Ensure logs directory exists
            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            Path csvFilePath = logsDir.resolve(csvFileName);
            Path zipFilePath = logsDir.resolve(zipFileName);

            // Write CSV file
            writeCsvFile(oldLogs, csvFilePath);

            // Create ZIP file
            createZipFile(csvFilePath, zipFilePath, csvFileName);

            // Delete temporary CSV file
            Files.deleteIfExists(csvFilePath);

            log.info("Exported {} error logs to {}", oldLogs.size(), zipFilePath.toAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to export old error logs", e);
        }
    }

    /**
     * Write error logs to CSV file
     */
    private void writeCsvFile(List<ErrorLog> logs, Path filePath) throws IOException {
        try (CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(filePath), CSVFormat.DEFAULT.withHeader(
                "ID", "Trace ID", "Service Name", "Error Code", "Error Message", "Request Data",
                "Response Data", "Status Code", "User ID", "Endpoint", "Method",
                "IP Address", "User Agent", "Stack Trace", "Created At"))) {

            for (ErrorLog log : logs) {
                csvPrinter.printRecord(
                    log.getId(),
                    log.getTraceId(),
                    log.getServiceName(),
                    log.getErrorCode(),
                    cleanCsvField(log.getErrorMessage()),
                    cleanCsvField(log.getRequestData()),
                    cleanCsvField(log.getResponseData()),
                    log.getStatusCode(),
                    log.getUserId(),
                    log.getEndpoint(),
                    log.getMethod(),
                    log.getIpAddress(),
                    cleanCsvField(log.getUserAgent()),
                    cleanCsvField(log.getStackTrace()),
                    log.getCreatedAt() != null
                            ? timestampMapper.toRequestUserTimeFormatted(
                                    log.getCreatedAt(), log.getCreatedTimeOffset(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : ""
                );
            }
        }
    }

    /**
     * Create ZIP file containing the CSV
     */
    private void createZipFile(Path csvFile, Path zipFile, String csvFileName) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            ZipEntry zipEntry = new ZipEntry(csvFileName);
            zipOut.putNextEntry(zipEntry);

            Files.copy(csvFile, zipOut);
            zipOut.closeEntry();
        }
    }

    /**
     * Clean CSV field to handle newlines and special characters
     */
    private String cleanCsvField(String field) {
        if (field == null) return "";
        // Replace newlines with spaces and limit length for CSV readability
        return field.replace("\n", " ").replace("\r", " ").trim();
    }

    /**
     * Capture request context data immediately to avoid losing it in async processing
     */
    private RequestContextData captureRequestContext() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return new RequestContextData(
                    request.getRequestURI(),
                    request.getMethod(),
                    getClientIpAddress(request),
                    request.getHeader("User-Agent")
                );
            }
        } catch (Exception e) {
            log.debug("Could not capture request context", e);
        }
        return new RequestContextData(null, null, null, null);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * Convert to JSON safely with size limit to prevent memory issues
     */
    private String convertToJsonSafely(Object obj, int maxSize) {
        if (obj == null) return null;
        try {
            String json;
            if (obj instanceof String) {
                json = (String) obj;
            } else {
                json = objectMapper.writeValueAsString(obj);
            }
            return truncateIfNeeded(json, maxSize);
        } catch (Exception e) {
            // Fallback to toString if JSON conversion fails
            return truncateIfNeeded(obj.toString(), maxSize);
        }
    }

    /**
     * Get stack trace safely with size limit
     */
    private String getStackTraceSafely(Exception e, int maxSize) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return truncateIfNeeded(sw.toString(), maxSize);
        } catch (Exception ex) {
            return "Failed to capture stack trace: " + ex.getMessage();
        }
    }

    /**
     * Truncate string if it exceeds maximum size
     */
    private String truncateIfNeeded(String str, int maxSize) {
        if (str == null || str.length() <= maxSize) {
            return str;
        }
        return str.substring(0, maxSize - 3) + "...";
    }

    /**
         * Inner class to hold request context data
         */
        private record RequestContextData(String endpoint, String method, String ipAddress, String userAgent) {
    }
}
