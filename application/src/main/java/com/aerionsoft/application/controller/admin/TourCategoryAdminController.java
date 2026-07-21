package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.tour.TourCategoryRequest;
import com.aerionsoft.application.dto.tour.TourCategoryResponse;
import com.aerionsoft.application.service.tour.TourCategoryService;
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
@RequestMapping("/api/admin/tour-categories")
@RequiredArgsConstructor
public class TourCategoryAdminController {

    private final TourCategoryService tourCategoryService;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-tour-category')")
    public ResponseEntity<BaseResponse<TourCategoryResponse>> create(
            @Valid @RequestBody TourCategoryRequest request) {
        TourCategoryResponse response = tourCategoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Tour category created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-tour-category')")
    public ResponseEntity<BaseResponse<TourCategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TourCategoryRequest request) {
        TourCategoryResponse response = tourCategoryService.update(id, request);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour category updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tour-category')")
    public ResponseEntity<BaseResponse<TourCategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok(
                tourCategoryService.getById(id), "Tour category retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-tour-category')")
    public ResponseEntity<BaseResponse<List<TourCategoryResponse>>> getAll(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(defaultValue = "false") boolean withTours) {
        List<TourCategoryResponse> response = withTours
                ? tourCategoryService.getAllWithTours(activeOnly)
                : tourCategoryService.getAll(activeOnly);
        return ResponseEntity.ok(BaseResponse.ok(response, "Tour categories retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-tour-category')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id) {
        tourCategoryService.delete(id);
        return ResponseEntity.ok(BaseResponse.ok("Tour category deleted successfully"));
    }
}
