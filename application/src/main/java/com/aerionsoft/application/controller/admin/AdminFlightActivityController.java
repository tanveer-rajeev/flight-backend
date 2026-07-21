package com.aerionsoft.application.controller.admin;

import org.springframework.validation.annotation.Validated;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.booking.BookingTimelineDTO;
import com.aerionsoft.application.service.booking.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoints to inspect per-user flight journey activity (search → validation → book → ticket).
 */
@RestController
@Validated
@RequestMapping("/api/admin/flight-activity")
public class AdminFlightActivityController extends BaseController {

    @Autowired
    private BookingService bookingService;

    /**
     * Full journey for one search session (search, price-validation, bundle, cart, then booking events if linked).
     * GET /api/admin/flight-activity/session?sessionId=xxx
     */
    @GetMapping("/session")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-booking')")
    public ResponseEntity<BaseResponse<List<BookingTimelineDTO>>> getBySession(
            @RequestParam String sessionId) {
        List<BookingTimelineDTO> timeline = bookingService.getFlightActivityBySession(sessionId);
        return ResponseEntity.ok(BaseResponse.ok("Flight activity timeline fetched", timeline));
    }

    /**
     * Recent flight-related steps for a user across sessions.
     * GET /api/admin/flight-activity/user?userId=123&limit=50
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('admin') or @permissionService.hasPermission(authentication, 'view-booking')")
    public ResponseEntity<BaseResponse<List<BookingTimelineDTO>>> getByUser(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        List<BookingTimelineDTO> timeline = bookingService.getFlightActivityByUser(userId, limit);
        return ResponseEntity.ok(BaseResponse.ok("User flight activity fetched", timeline));
    }
}
