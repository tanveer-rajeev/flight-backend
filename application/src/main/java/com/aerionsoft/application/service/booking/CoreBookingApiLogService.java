package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.entity.Booking.CoreBookingApiLog;
import com.aerionsoft.application.repository.booking.CoreBookingApiLogRepository;
import com.aerionsoft.application.service.user.CustomUserDetails;
import com.aerionsoft.application.util.TraceIdSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CoreBookingApiLogService {

    private static final int MAX_JSON_LENGTH = 50000;

    private final CoreBookingApiLogRepository coreBookingApiLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoreBookingApiLogService(CoreBookingApiLogRepository coreBookingApiLogRepository) {
        this.coreBookingApiLogRepository = coreBookingApiLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            String endpoint,
            String sessionId,
            Object request,
            Object response,
            String httpStatus,
            String status,
            String errorMessage) {
        try {
            CoreBookingApiLog entry = CoreBookingApiLog.builder()
                    .userId(resolveCurrentUserId())
                    .traceId(TraceIdSupport.currentTraceId())
                    .endpoint(endpoint)
                    .sessionId(sessionId)
                    .requestData(toJson(request))
                    .responseData(toJson(response))
                    .httpStatus(httpStatus)
                    .status(status)
                    .errorMessage(truncate(errorMessage, 10000))
                    .build();

            coreBookingApiLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to persist core booking API log. endpoint={}, sessionId={}", endpoint, sessionId, e);
        }
    }

    private static Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        return null;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(value), MAX_JSON_LENGTH);
        } catch (Exception e) {
            return truncate(String.valueOf(value), MAX_JSON_LENGTH);
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
