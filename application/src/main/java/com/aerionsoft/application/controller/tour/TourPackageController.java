package com.aerionsoft.application.controller.tour;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.tour.TourPackageRequest;
import com.aerionsoft.application.dto.tour.TourPackageResponse;
import com.aerionsoft.application.dto.tour.TourPackageSearchResponse;
import com.aerionsoft.application.service.admin.AdminUserService;
import com.aerionsoft.application.service.tour.TourPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/tour-packages")
@RequiredArgsConstructor
public class TourPackageController {

    private final TourPackageService tourPackageService;
    private final AdminUserService adminUserService;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-tour-package')")
    public ResponseEntity<BaseResponse<TourPackageResponse>> createTourPackage(
            @Valid @RequestBody TourPackageRequest request,
            Authentication authentication) {
        Long adminId = adminUserService.getUserByEmail(authentication.getName()).getId();
        TourPackageResponse response = tourPackageService.createTourPackage(request, adminId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour package created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-tour-package')")
    public ResponseEntity<BaseResponse<TourPackageResponse>> updateTourPackage(
            @PathVariable Long id,
            @Valid @RequestBody TourPackageRequest request) {
        TourPackageResponse response = tourPackageService.updateTourPackage(id, request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour package updated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<TourPackageResponse>> getTourPackageById(@PathVariable Long id) {
        TourPackageResponse response = tourPackageService.getTourPackageById(id);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour package retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<Page<TourPackageResponse>>> getAllTourPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TourPackageResponse> response = tourPackageService.getAllTourPackages(pageable);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<BaseResponse<List<TourPackageResponse>>> getTourPackagesByStatus(
            @PathVariable String status) {
        List<TourPackageResponse> response = tourPackageService.getTourPackagesByStatus(status);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages by status retrieved successfully"));
    }

    @GetMapping("/destination")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tour-package')") // admin or user
    public ResponseEntity<BaseResponse<List<TourPackageResponse>>> getTourPackagesByDestination(
            @RequestParam String country,
            @RequestParam(required = false) String city) {
        List<TourPackageResponse> response = tourPackageService.getTourPackagesByDestination(country, city);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages by destination retrieved successfully"));
    }

    @GetMapping("/date-range")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tour-package')") // admin or user
    public ResponseEntity<BaseResponse<List<TourPackageResponse>>> getTourPackagesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TourPackageResponse> response = tourPackageService.getTourPackagesByDateRange(startDate, endDate);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages by date range retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tour-package')") // admin or user
    public ResponseEntity<BaseResponse<TourPackageSearchResponse>> searchTourPackages(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        TourPackageSearchResponse response = tourPackageService.searchTourPackages(keyword, pageable);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour packages search results retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-tour-package')")
    public ResponseEntity<BaseResponse<Void>> deleteTourPackage(@PathVariable Long id) {
        tourPackageService.deleteTourPackage(id);
        return ResponseEntity.ok(BaseResponse.ok((Void) null, "Tour package deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-tour-package')")
    public ResponseEntity<BaseResponse<Void>> changePackageStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        tourPackageService.changePackageStatus(id, status);
        return ResponseEntity.ok(BaseResponse.ok((Void) null, "Tour package status updated successfully"));
    }
}
