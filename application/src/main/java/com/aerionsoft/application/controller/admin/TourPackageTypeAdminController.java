package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.tour.TourPackageTypeRequest;
import com.aerionsoft.application.dto.tour.TourPackageTypeResponse;
import com.aerionsoft.application.service.tour.TourPackageTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/admin/tour-package-types")
@RequiredArgsConstructor
public class TourPackageTypeAdminController {

    private final TourPackageTypeService tourPackageTypeService;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-tour-package-type')")
    public ResponseEntity<BaseResponse<TourPackageTypeResponse>> create(
            @Valid @RequestBody TourPackageTypeRequest request) {
        TourPackageTypeResponse response = tourPackageTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Tour package type created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-tour-package-type')")
    public ResponseEntity<BaseResponse<TourPackageTypeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TourPackageTypeRequest request) {
        TourPackageTypeResponse response = tourPackageTypeService.update(id, request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour package type updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tour-package-type')")
    public ResponseEntity<BaseResponse<TourPackageTypeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok(tourPackageTypeService.getById(id),
                "Tour package type retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tour-package-type')")
    public ResponseEntity<BaseResponse<List<TourPackageTypeResponse>>> getAll(
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(BaseResponse.ok(tourPackageTypeService.getAll(activeOnly),
                "Tour package types retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-tour-package-type')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id) {
        tourPackageTypeService.delete(id);
        return ResponseEntity.ok(BaseResponse.ok("Tour package type deleted successfully"));
    }
}
