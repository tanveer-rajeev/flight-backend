package com.aerionsoft.application.service.errorlog;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.ErrorLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persists error logs with native SQL so inserts work against the partitioned
 * {@code error_log} table (composite PK in PostgreSQL, single id in JPA reads).
 */
@Service
public class ErrorLogPersistenceService {

    private static final String INSERT_SQL = """
            INSERT INTO error_log (
                created_at,
                service_name,
                error_code,
                error_message,
                request_data,
                response_data,
                status_code,
                user_id,
                trace_id,
                endpoint,
                method,
                ip_address,
                user_agent,
                stack_trace
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long insert(ErrorLog errorLog) {
        LocalDateTime createdAt = errorLog.getCreatedAt() != null
                ? errorLog.getCreatedAt()
                : UserDateTimeUtil.now();

        Long id = jdbcTemplate.queryForObject(
                INSERT_SQL,
                Long.class,
                createdAt,
                errorLog.getServiceName(),
                errorLog.getErrorCode(),
                errorLog.getErrorMessage(),
                errorLog.getRequestData(),
                errorLog.getResponseData(),
                errorLog.getStatusCode(),
                errorLog.getUserId(),
                errorLog.getTraceId(),
                errorLog.getEndpoint(),
                errorLog.getMethod(),
                errorLog.getIpAddress(),
                errorLog.getUserAgent(),
                errorLog.getStackTrace()
        );

        errorLog.setId(id);
        errorLog.setCreatedAt(createdAt);
        return id;
    }
}
