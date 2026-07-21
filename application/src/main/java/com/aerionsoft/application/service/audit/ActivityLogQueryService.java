package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.dto.audit.ActivityLogResponse;
import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.enums.audit.ActivityEventCategory;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActorType;
import com.aerionsoft.application.repository.audit.ActivityLogRepository;
import com.aerionsoft.application.repository.audit.ActivityLogSpec;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ActivityLogQueryService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    public Page<ActivityLogResponse> search(
            ActorType actorType,
            Long actorId,
            ActivityEventCategory eventCategory,
            ActivityEventType eventType,
            String resourceType,
            String resourceId,
            String traceId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        Specification<ActivityLog> spec = Specification
                .where(ActivityLogSpec.hasActorType(actorType))
                .and(ActivityLogSpec.hasActorId(actorId))
                .and(ActivityLogSpec.hasEventCategory(eventCategory))
                .and(ActivityLogSpec.hasEventType(eventType))
                .and(ActivityLogSpec.hasResourceType(normalize(resourceType)))
                .and(ActivityLogSpec.hasResourceId(normalize(resourceId)))
                .and(ActivityLogSpec.hasTraceId(normalize(traceId)))
                .and(ActivityLogSpec.createdAtFrom(fromDate))
                .and(ActivityLogSpec.createdAtTo(toDate));

        Pageable sortedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ActivityLog> page = activityLogRepository.findAll(spec, sortedPageable);
        return mapPage(page);
    }

    public Page<ActivityLogResponse> findByActor(ActorType actorType, Long actorId, Pageable pageable) {
        Page<ActivityLog> page = activityLogRepository
                .findByActorTypeAndActorIdOrderByCreatedAtDesc(actorType, actorId, pageable);
        return mapPage(page);
    }

    private Page<ActivityLogResponse> mapPage(Page<ActivityLog> page) {
        NameLookup lookup = buildNameLookup(page.getContent());
        return page.map(log -> toResponse(log, lookup));
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
            if (log.getImpersonatedByAdminId() != null) {
                adminIds.add(log.getImpersonatedByAdminId());
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

    private record NameLookup(Map<Long, String> userNames, Map<Long, String> adminNames) {
    }

    public Optional<ActivityLogResponse> findById(Long id) {
        return activityLogRepository.findById(id).map(log -> toResponse(log, buildNameLookup(List.of(log))));
    }

    private ActivityLogResponse toResponse(ActivityLog log, NameLookup lookup) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .eventCategory(log.getEventCategory())
                .outcome(log.getOutcome())
                .actorType(log.getActorType())
                .actorId(log.getActorId())
                .actorEmail(log.getActorEmail())
                .actorName(resolveActorName(log.getActorType(), log.getActorId(), lookup))
                .impersonatedByAdminId(log.getImpersonatedByAdminId())
                .impersonatedByAdminName(resolveAdminName(log.getImpersonatedByAdminId(), lookup))
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .description(log.getDescription())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .traceId(log.getTraceId())
                .httpMethod(log.getHttpMethod())
                .httpPath(log.getHttpPath())
                .createdAt(timestampMapper.toRequestUserTime(log.getCreatedAt(), log.getCreatedTimeOffset()))
                .build();
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

    private String resolveAdminName(Long adminId, NameLookup lookup) {
        if (adminId == null) {
            return null;
        }
        return lookup.adminNames().get(adminId);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
