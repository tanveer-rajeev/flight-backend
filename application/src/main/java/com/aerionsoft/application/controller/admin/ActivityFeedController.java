package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.audit.ActivityFeedEvent;
import com.aerionsoft.application.enums.audit.ActivityEventCategory;
import com.aerionsoft.application.service.audit.ActivityFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/admin/activity-feed")
public class ActivityFeedController extends BaseController {

    private final ActivityFeedService activityFeedService;

    public ActivityFeedController(ActivityFeedService activityFeedService) {
        this.activityFeedService = activityFeedService;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-activity-log')")
    public ResponseEntity<BaseResponse<List<ActivityFeedEvent>>> getFeed(
            @RequestParam(required = false) Long sinceId,
            @RequestParam(required = false) String categories,
            @RequestParam(defaultValue = "50") int limit) {

        List<ActivityEventCategory> parsedCategories = parseCategories(categories);
        List<ActivityFeedEvent> events = activityFeedService.getFeed(sinceId, parsedCategories, limit);
        return ResponseEntity.ok(BaseResponse.ok("Activity feed retrieved successfully", events));
    }

    private List<ActivityEventCategory> parseCategories(String categories) {
        if (categories == null || categories.isBlank()) {
            return List.of();
        }
        return Arrays.stream(categories.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .map(ActivityEventCategory::valueOf)
                .toList();
    }
}
