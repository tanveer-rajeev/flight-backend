package com.aerionsoft.application.dto.audit;

import com.aerionsoft.application.enums.audit.ActivityEventCategory;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.enums.audit.ActorType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ActivityFeedEvent {

    private Long id;
    private ActivityEventType eventType;
    private ActivityEventCategory eventCategory;
    private ActivityOutcome outcome;
    private String description;
    private String summary;
    private ActorType actorType;
    private Long actorId;
    private String actorEmail;
    private String actorName;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> metadata;
    private ActivityFeedAgencyInfo agency;
    private ActivityFeedDetailsInfo details;
    private String traceId;
    private LocalDateTime createdAt;
}
