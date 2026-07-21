package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.util.ActorContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ActivityAuthAuditSupport {

    private final ActivityLogService activityLogService;

    public ActivityAuthAuditSupport(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    public void logUserLogin(User user, String ip, String userAgent) {
        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.USER_LOGIN)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.forUser(user.getId(), user.getEmail()))
                .resourceType("USER")
                .resourceId(String.valueOf(user.getId()))
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
    }

    public void logAdminLogin(AdminUser adminUser, String ip, String userAgent) {
        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.ADMIN_LOGIN)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.forAdmin(adminUser.getId(), adminUser.getEmail()))
                .resourceType("ADMIN_USER")
                .resourceId(String.valueOf(adminUser.getId()))
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
    }

    // Disabled for now — re-enable when auth audit scope is finalized.
    // public void logTokenRefresh(ActorContext actor, String ip, String userAgent) {
    //     activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
    //             .eventType(ActivityEventType.TOKEN_REFRESH)
    //             .outcome(ActivityOutcome.SUCCESS)
    //             .actor(actor)
    //             .resourceType(actor.getType().name())
    //             .resourceId(actor.getId() != null ? String.valueOf(actor.getId()) : null)
    //             .ipAddress(ip)
    //             .userAgent(userAgent)
    //             .build());
    // }

    public void logImpersonation(AdminUser adminUser, User user, String ip, String userAgent, String reason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("targetUserEmail", user.getEmail());
        metadata.put("targetUserName", user.getFullName());
        if (reason != null && !reason.isBlank()) {
            metadata.put("reason", reason);
        }

        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.ADMIN_IMPERSONATE)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.forAdmin(adminUser.getId(), adminUser.getEmail()))
                .resourceType("USER")
                .resourceId(String.valueOf(user.getId()))
                .metadata(metadata)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
    }

    public void logLoginFailed(String email, String reason, boolean adminPortal, String ip, String userAgent) {
        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.LOGIN_FAILED)
                .outcome(ActivityOutcome.FAILURE)
                .actor(ActorContext.guest())
                .description(reason)
                .metadata(Map.of(
                        "email", email != null ? email : "",
                        "portal", adminPortal ? "admin" : "user"))
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
    }

    public void logPasswordReset(ActorContext actor, String resourceType, Long resourceId) {
        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.PASSWORD_RESET)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(actor)
                .resourceType(resourceType)
                .resourceId(resourceId != null ? String.valueOf(resourceId) : null)
                .build());
    }

    public void logPasswordChange(ActorContext actor, String resourceType, Long resourceId) {
        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.PASSWORD_CHANGE)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(actor)
                .resourceType(resourceType)
                .resourceId(resourceId != null ? String.valueOf(resourceId) : null)
                .build());
    }

    // Disabled for now — re-enable when auth audit scope is finalized.
    // public void logUserRegistered(Long userId, String email) {
    //     activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
    //             .eventType(ActivityEventType.USER_REGISTERED)
    //             .outcome(ActivityOutcome.SUCCESS)
    //             .actor(ActorContext.guest())
    //             .resourceType("USER")
    //             .resourceId(String.valueOf(userId))
    //             .metadata(Map.of("email", email != null ? email : ""))
    //             .build());
    // }
}
