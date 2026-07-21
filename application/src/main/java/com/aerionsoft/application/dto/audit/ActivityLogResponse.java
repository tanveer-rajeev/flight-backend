package com.aerionsoft.application.dto.audit;

import com.aerionsoft.application.enums.audit.ActivityEventCategory;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.enums.audit.ActorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityLogResponse {
    private Long id;
    private ActivityEventType eventType;
    private ActivityEventCategory eventCategory;
    private ActivityOutcome outcome;
    private ActorType actorType;
    private Long actorId;
    private String actorEmail;
    private String actorName;
    private Long impersonatedByAdminId;
    private String impersonatedByAdminName;
    private String resourceType;
    private String resourceId;
    private String description;
    private String metadata;
    private String ipAddress;
    private String userAgent;
    private String traceId;
    private String httpMethod;
    private String httpPath;
    private LocalDateTime createdAt;
}
