package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.admin.summery.*;
import org.springframework.validation.annotation.Validated;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.admin.SummeryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@Validated
@RequestMapping("/api/admin/summery")
public class SummeryController extends BaseController {

    @Autowired
    private SummeryService summeryService;


    @GetMapping("")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<HashMap<String, Object>>> getStatement() {
        HashMap<String, Object> map = summeryService.getStatement();
        return ResponseEntity.ok(BaseResponse.ok("Summery List", map));
    }

    @GetMapping("/today")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-statement')")
    public ResponseEntity<BaseResponse<HashMap<String, Object>>> getTodayStatement() {
        HashMap<String, Object> map = summeryService.getTodayStatement();
        return ResponseEntity.ok(BaseResponse.ok("Today's Summery", map));
    }


    @GetMapping("/recent-activities")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-recent-activity')")
    public ResponseEntity<BaseResponse<RecentActivity>> getRecentActivities() {

        RecentActivity activities = summeryService.getRecentActivities();

        return ResponseEntity.ok(BaseResponse.ok("Recent Activities", activities));
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = summeryService.getDashboardStats();
        return ResponseEntity.ok(BaseResponse.ok("Dashboard Statistics", stats));
    }

    @GetMapping("/active-users")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<ActiveUsersResponse>> getActiveUsers() {
        ActiveUsersResponse activeUsers = summeryService.getActiveUsers();
        return ResponseEntity.ok(BaseResponse.ok("Active Users (online now)", activeUsers));
    }

    @GetMapping("/active-users/count")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<HashMap<String, Long>>> getActiveUsersCount() {
        HashMap<String, Long> count = summeryService.getActiveUsersCount();
        return ResponseEntity.ok(BaseResponse.ok("Active Users Count", count));
    }

    @GetMapping("/top-routes/booked")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<TopRoutesResponse>> getTopBookedRoutes(
            @RequestParam(defaultValue = "10") int limit) {
        TopRoutesResponse routes = summeryService.getTopBookedRoutes(limit);
        return ResponseEntity.ok(BaseResponse.ok("Top Booked Routes", routes));
    }

    @GetMapping("/top-routes/searched")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<TopRoutesResponse>> getTopSearchedRoutes(
            @RequestParam(defaultValue = "10") int limit) {
        TopRoutesResponse routes = summeryService.getTopSearchedRoutes(limit);
        return ResponseEntity.ok(BaseResponse.ok("Top Searched Routes", routes));
    }

    @GetMapping("/top-airlines")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<TopAirlinesResponse>> getTopBookedAirlines(
            @RequestParam(defaultValue = "10") int limit) {
        TopAirlinesResponse airlines = summeryService.getTopBookedAirlines(limit);
        return ResponseEntity.ok(BaseResponse.ok("Top Airlines by Booking Count", airlines));
    }

    @GetMapping("/top-destinations/booked")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<TopDestinationsResponse>> getTopBookedDestinations(
            @RequestParam(defaultValue = "10") int limit) {
        TopDestinationsResponse destinations = summeryService.getTopBookedDestinations(limit);
        return ResponseEntity.ok(BaseResponse.ok("Top Booked Destinations", destinations));
    }

    @GetMapping("/top-destinations/searched")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-summery')")
    public ResponseEntity<BaseResponse<TopDestinationsResponse>> getTopSearchedDestinations(
            @RequestParam(defaultValue = "10") int limit) {
        TopDestinationsResponse destinations = summeryService.getTopSearchedDestinations(limit);
        return ResponseEntity.ok(BaseResponse.ok("Top Searched Destinations", destinations));
    }

}
