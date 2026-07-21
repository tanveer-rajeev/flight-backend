package com.aerionsoft.application.repository.audit;

import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.enums.audit.ActivityEventCategory;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActorType;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class ActivityLogSpec {

    private ActivityLogSpec() {
    }

    public static Specification<ActivityLog> hasActorType(ActorType actorType) {
        return (root, query, cb) ->
                actorType == null ? null : cb.equal(root.get("actorType"), actorType);
    }

    public static Specification<ActivityLog> hasActorId(Long actorId) {
        return (root, query, cb) ->
                actorId == null ? null : cb.equal(root.get("actorId"), actorId);
    }

    public static Specification<ActivityLog> hasEventCategory(ActivityEventCategory eventCategory) {
        return (root, query, cb) ->
                eventCategory == null ? null : cb.equal(root.get("eventCategory"), eventCategory);
    }

    public static Specification<ActivityLog> hasEventType(ActivityEventType eventType) {
        return (root, query, cb) ->
                eventType == null ? null : cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<ActivityLog> hasResourceType(String resourceType) {
        return (root, query, cb) ->
                resourceType == null ? null : cb.equal(root.get("resourceType"), resourceType);
    }

    public static Specification<ActivityLog> hasResourceId(String resourceId) {
        return (root, query, cb) ->
                resourceId == null ? null : cb.equal(root.get("resourceId"), resourceId);
    }

    public static Specification<ActivityLog> hasTraceId(String traceId) {
        return (root, query, cb) ->
                traceId == null ? null : cb.equal(root.get("traceId"), traceId);
    }

    public static Specification<ActivityLog> createdAtFrom(LocalDateTime fromDate) {
        return OffsetAwareDateSpec.createdAtFromUserLocalDateTimes(
                fromDate, null, "createdAt", "createdTimeOffset");
    }

    public static Specification<ActivityLog> createdAtTo(LocalDateTime toDate) {
        return OffsetAwareDateSpec.createdAtFromUserLocalDateTimes(
                null, toDate, "createdAt", "createdTimeOffset");
    }
}
