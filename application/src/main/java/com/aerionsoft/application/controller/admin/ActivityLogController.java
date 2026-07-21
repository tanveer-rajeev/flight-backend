package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.audit.ActivityLogResponse;
import com.aerionsoft.application.enums.audit.ActivityEventCategory;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActorType;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.service.audit.ActivityLogQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Validated
@RequestMapping("/api/admin/activity-log")
public class ActivityLogController extends BaseController {

    @Autowired
    private ActivityLogQueryService activityLogQueryService;

    @GetMapping
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-activity-log')")
    public ResponseEntity<BaseResponse<Page<ActivityLogResponse>>> search(
            @RequestParam(required = false) ActorType actorType,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) ActivityEventCategory eventCategory,
            @RequestParam(required = false) ActivityEventType eventType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200));
        Page<ActivityLogResponse> results = activityLogQueryService.search(
                actorType, actorId, eventCategory, eventType,
                resourceType, resourceId, traceId, from, to, pageable);
        return ResponseEntity.ok(BaseResponse.ok("Activity logs retrieved successfully", results));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-activity-log')")
    public ResponseEntity<BaseResponse<Page<ActivityLogResponse>>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200));
        Page<ActivityLogResponse> results =
                activityLogQueryService.findByActor(ActorType.USER, userId, pageable);
        return ResponseEntity.ok(BaseResponse.ok("User activity logs retrieved successfully", results));
    }

    @GetMapping("/admins/{adminId}")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-activity-log')")
    public ResponseEntity<BaseResponse<Page<ActivityLogResponse>>> getByAdmin(
            @PathVariable Long adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200));
        Page<ActivityLogResponse> results =
                activityLogQueryService.findByActor(ActorType.ADMIN, adminId, pageable);
        return ResponseEntity.ok(BaseResponse.ok("Admin activity logs retrieved successfully", results));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-activity-log')")
    public ResponseEntity<BaseResponse<ActivityLogResponse>> getById(@PathVariable Long id) {
        ActivityLogResponse response = activityLogQueryService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity log not found with id: " + id));
        return ResponseEntity.ok(BaseResponse.ok("Activity log retrieved successfully", response));
    }
}
