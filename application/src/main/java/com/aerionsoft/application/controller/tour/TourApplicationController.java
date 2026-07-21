package com.aerionsoft.application.controller.tour;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.tour.TourApplicationRequest;
import com.aerionsoft.application.dto.tour.TourApplicationResponse;
import com.aerionsoft.application.enums.tour.ApplicationStatus;
import com.aerionsoft.application.service.tour.TourApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/tour-applications")
public class TourApplicationController extends BaseController{

    @Autowired
    private TourApplicationService tourApplicationService;


    @GetMapping("/my-tours")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tour-package')") // user
    public ResponseEntity<BaseResponse<List<TourApplicationResponse>>> getMyTourPackages() {
        Long userId = getUserIdFromAuthentication();
        List<TourApplicationResponse> response = tourApplicationService.getMyTourPackages(userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour package retrieved successfully"));
    }


    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-application')") // admin or user
    public ResponseEntity<BaseResponse<TourApplicationResponse>> createApplication(@Valid @RequestBody TourApplicationRequest request) {
        Long userId = getUserIdFromAuthentication();
        TourApplicationResponse application = tourApplicationService.createApplicationTour(request,userId);
        return ResponseEntity.ok(BaseResponse.ok(application, "Tour application created successfully"));

    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-application')") // admin or user
    public ResponseEntity<BaseResponse<TourApplicationResponse>> getApplication(@PathVariable Long id) {
            TourApplicationResponse application = tourApplicationService.getApplicationById(id);
            return ResponseEntity.ok(BaseResponse.ok(application, "Tour application retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-application')") // admin or user
    public ResponseEntity<BaseResponse<List<TourApplicationResponse>>> getAllApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Long formId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedTo,
            @RequestParam(required = false) String processedBy) {

        // If any filter parameters are provided, use Specification-based filtered search
        if (status != null || tourId != null || formId != null ||
                submittedFrom != null || submittedTo != null || processedBy != null) {
            List<TourApplicationResponse> applications = tourApplicationService.getAllApplicationsWithAdvancedFilters(
                    status, tourId, formId, submittedFrom, submittedTo, processedBy);
            return ResponseEntity.ok(BaseResponse.ok(applications, "Filtered tour applications retrieved successfully"));
        }

        // Otherwise, use the existing method for backward compatibility
        List<TourApplicationResponse> applications = tourApplicationService.getAllTourApplications();
        return ResponseEntity.ok(BaseResponse.ok(applications, "Tour applications retrieved successfully"));
    }


    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-application-status')")
    public ResponseEntity<BaseResponse<TourApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            @RequestParam(required = false) String remarks) {
            TourApplicationResponse application = tourApplicationService.updateApplicationStatus(id, status, remarks);
            return ResponseEntity.ok(BaseResponse.ok(application, "Tour application status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-application')")
    public ResponseEntity<BaseResponse<Void>> deleteApplication(@PathVariable Long id) {
            tourApplicationService.deleteApplication(id);
            return ResponseEntity.ok(BaseResponse.ok("Tour application deleted successfully"));
    }
}
