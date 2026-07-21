package com.aerionsoft.application.repository.common;

import com.aerionsoft.application.entity.ErrorLog;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class ErrorLogSpec {

    private ErrorLogSpec() {}

    public static Specification<ErrorLog> hasTraceId(String traceId) {
        return (root, query, cb) ->
                traceId == null ? null : cb.equal(root.get("traceId"), traceId);
    }

    public static Specification<ErrorLog> hasUserId(Long userId) {
        return (root, query, cb) ->
                userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<ErrorLog> hasServiceName(String serviceName) {
        return (root, query, cb) ->
                serviceName == null ? null : cb.equal(root.get("serviceName"), serviceName);
    }

    public static Specification<ErrorLog> hasErrorCode(String errorCode) {
        return (root, query, cb) ->
                errorCode == null ? null : cb.equal(root.get("errorCode"), errorCode);
    }

    public static Specification<ErrorLog> createdBetween(LocalDateTime from, LocalDateTime to) {
        return OffsetAwareDateSpec.createdAtFromUserLocalDateTimes(
                from, to, "createdAt", "createdTimeOffset");
    }
}
