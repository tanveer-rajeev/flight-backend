package com.aerionsoft.application.entity;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorLog {

    @Id
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "status_code", length = 10)
    private String statusCode;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "trace_id", length = 36)
    private String traceId;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "method", length = 10)
    private String method;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = UserDateTimeUtil.now();
        }
    }
}
