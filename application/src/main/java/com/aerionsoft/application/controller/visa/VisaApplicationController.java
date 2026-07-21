package com.aerionsoft.application.controller.visa;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.visa.VisaApplicationDetailResponse;
import com.aerionsoft.application.dto.visa.VisaApplicationRequest;
import com.aerionsoft.application.dto.visa.VisaApplicationResponse;
import com.aerionsoft.application.enums.tour.ApplicationStatus;
import com.aerionsoft.application.service.visa.VisaApplicationService;
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
@RequestMapping("/api/visa-applications")
public class VisaApplicationController extends BaseController{

    @Autowired
    private VisaApplicationService visaApplicationService;


    @GetMapping("/my-visas")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-visa-application')")
    public ResponseEntity<BaseResponse<List<VisaApplicationResponse>>> getMyVisaApplications() {
        Long userId = getUserIdFromAuthentication();
        List<VisaApplicationResponse> response = visaApplicationService.getMyVisaApplications(userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Visa applications retrieved successfully"));
    }



    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-visa-application')") // admin or user
    public ResponseEntity<BaseResponse<VisaApplicationResponse>> createApplication(@Valid @RequestBody VisaApplicationRequest request) {
        Long userId = getUserIdFromAuthentication();
        VisaApplicationResponse application = visaApplicationService.createApplication(request,userId);
        return ResponseEntity.ok(BaseResponse.ok(application, "Visa application created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-visa-application')") // admin or user
    public ResponseEntity<BaseResponse<VisaApplicationDetailResponse>> getApplication(@PathVariable Long id) {
        VisaApplicationDetailResponse application = visaApplicationService.getApplicationDetailById(id);
        return ResponseEntity.ok(BaseResponse.ok(application, "Visa application retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-visa-application')") // admin or user
    public ResponseEntity<BaseResponse<List<VisaApplicationResponse>>> getAllApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String visaType,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedTo,
            @RequestParam(required = false) String processedBy,
            @RequestParam(required = false) Long visaId) {

        // If any filter parameters are provided, use filtered search
        if (status != null || visaType != null || country != null ||
            submittedFrom != null || submittedTo != null || processedBy != null || visaId != null) {
            List<VisaApplicationResponse> applications = visaApplicationService.getAllApplicationsWithFilters(
                status, visaType, country, submittedFrom, submittedTo, processedBy, visaId);
            return ResponseEntity.ok(BaseResponse.ok(applications, "Filtered visa applications retrieved successfully"));
        }

        // Otherwise, use the existing method for backward compatibility
        List<VisaApplicationResponse> applications = visaApplicationService.getAllApplications();
        return ResponseEntity.ok(BaseResponse.ok(applications, "Visa applications retrieved successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-visa-application')")
    public ResponseEntity<BaseResponse<VisaApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            @RequestParam(required = false) String remarks) {
        VisaApplicationResponse application = visaApplicationService.updateApplicationStatus(id, status, remarks);
        return ResponseEntity.ok(BaseResponse.ok(application, "Visa application status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-visa-application')")
    public ResponseEntity<BaseResponse<Void>> deleteApplication(@PathVariable Long id) {
        visaApplicationService.deleteApplication(id);
        return ResponseEntity.ok(BaseResponse.ok("Visa application deleted successfully"));
    }


}
