package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.dto.audit.ActivityFeedAgencyInfo;
import com.aerionsoft.application.dto.audit.ActivityFeedDetailsInfo;
import com.aerionsoft.application.dto.audit.ActivityFeedEvent;
import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.enums.audit.ActorType;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ActivityFeedMapper {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final TimestampMapper timestampMapper;
    private final ActivityFeedAgencyEnricher activityFeedAgencyEnricher;

    public ActivityFeedMapper(
            UserRepository userRepository,
            AdminUserRepository adminUserRepository,
            TimestampMapper timestampMapper,
            ActivityFeedAgencyEnricher activityFeedAgencyEnricher) {
        this.userRepository = userRepository;
        this.adminUserRepository = adminUserRepository;
        this.timestampMapper = timestampMapper;
        this.activityFeedAgencyEnricher = activityFeedAgencyEnricher;
    }

    public List<ActivityFeedEvent> toFeedEvents(List<ActivityLog> logs) {
        NameLookup lookup = buildNameLookup(logs);
        Map<Long, Map<String, Object>> metadataByLogId = new HashMap<>();
        for (ActivityLog log : logs) {
            metadataByLogId.put(log.getId(), ActivityFeedSummaryBuilder.parseMetadata(log.getMetadata()));
        }
        Map<Long, ActivityFeedAgencyInfo> agencies = activityFeedAgencyEnricher.resolveAgencies(logs, metadataByLogId);
        return logs.stream()
                .map(log -> toFeedEvent(log, lookup, metadataByLogId.get(log.getId()), agencies.get(log.getId())))
                .toList();
    }

    public ActivityFeedEvent toFeedEvent(ActivityLog log) {
        Map<String, Object> metadata = ActivityFeedSummaryBuilder.parseMetadata(log.getMetadata());
        Map<Long, Map<String, Object>> metadataByLogId = Map.of(log.getId(), metadata);
        ActivityFeedAgencyInfo agency = activityFeedAgencyEnricher.resolveAgencies(List.of(log), metadataByLogId)
                .get(log.getId());
        return toFeedEvent(log, buildNameLookup(List.of(log)), metadata, agency);
    }

    private ActivityFeedEvent toFeedEvent(
            ActivityLog log,
            NameLookup lookup,
            Map<String, Object> metadata,
            ActivityFeedAgencyInfo agency) {
        ActivityFeedDetailsInfo details = activityFeedAgencyEnricher.resolveDetails(metadata, log);
        return ActivityFeedEvent.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .eventCategory(log.getEventCategory())
                .outcome(log.getOutcome())
                .description(log.getDescription())
                .summary(ActivityFeedSummaryBuilder.build(log.getEventType(), metadata, agency))
                .actorType(log.getActorType())
                .actorId(log.getActorId())
                .actorEmail(log.getActorEmail())
                .actorName(resolveActorName(log.getActorType(), log.getActorId(), lookup))
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .metadata(metadata)
                .agency(agency)
                .details(details)
                .traceId(log.getTraceId())
                .createdAt(timestampMapper.toRequestUserTime(log.getCreatedAt(), log.getCreatedTimeOffset()))
                .build();
    }

    private NameLookup buildNameLookup(List<ActivityLog> logs) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> adminIds = new HashSet<>();
        for (ActivityLog log : logs) {
            if (log.getActorType() == ActorType.USER && log.getActorId() != null) {
                userIds.add(log.getActorId());
            } else if (log.getActorType() == ActorType.ADMIN && log.getActorId() != null) {
                adminIds.add(log.getActorId());
            }
        }

        Map<Long, String> userNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u -> userNames.put(u.getId(), u.getFullName()));
        }

        Map<Long, String> adminNames = new HashMap<>();
        if (!adminIds.isEmpty()) {
            adminUserRepository.findAllById(adminIds).forEach(a -> adminNames.put(a.getId(), a.getFullName()));
        }

        return new NameLookup(userNames, adminNames);
    }

    private String resolveActorName(ActorType actorType, Long actorId, NameLookup lookup) {
        if (actorId == null || actorType == null) {
            return null;
        }
        return switch (actorType) {
            case USER -> lookup.userNames().get(actorId);
            case ADMIN -> lookup.adminNames().get(actorId);
            default -> null;
        };
    }

    private record NameLookup(Map<Long, String> userNames, Map<Long, String> adminNames) {
    }
}
