package com.aerionsoft.application.entity.Booking;

import com.aerionsoft.application.util.UserDateTimeUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "core_booking_api_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreBookingApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "trace_id", length = 36)
    private String traceId;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "http_status", length = 10)
    private String httpStatus;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = UserDateTimeUtil.now();
        }
    }
}
