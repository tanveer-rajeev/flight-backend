package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.customform.CustomFormRequest;
import com.aerionsoft.application.dto.customform.CustomFormResponse;
import com.aerionsoft.application.service.admin.CustomFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/forms")
public class CustomFormController {

    @Autowired
    private CustomFormService customFormService;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-form')")
    public ResponseEntity<BaseResponse<CustomFormResponse>> createForm(@Valid @RequestBody CustomFormRequest request) {
        CustomFormResponse form = customFormService.createForm(request);
        return ResponseEntity.ok(BaseResponse.ok(form, "Form created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-form')")
    public ResponseEntity<BaseResponse<CustomFormResponse>> getForm(@PathVariable Long id) {
        CustomFormResponse form = customFormService.getFormById(id);
        return ResponseEntity.ok(BaseResponse.ok(form, "Form retrieved successfully"));
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-form')")
    public ResponseEntity<BaseResponse<CustomFormResponse>> getFormBySlug(@PathVariable String slug) {
        CustomFormResponse form = customFormService.getFormBySlug(slug);
        return ResponseEntity.ok(BaseResponse.ok(form, "Form retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-form')")
    public ResponseEntity<BaseResponse<List<CustomFormResponse>>> getAllForms() {
        List<CustomFormResponse> forms = customFormService.getAllForms();
        return ResponseEntity.ok(BaseResponse.ok(forms, "Forms retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-form')")
    public ResponseEntity<BaseResponse<CustomFormResponse>> updateForm(@PathVariable Long id, @Valid @RequestBody CustomFormRequest request) {
        CustomFormResponse form = customFormService.updateForm(id, request);
        return ResponseEntity.ok(BaseResponse.ok(form, "Form updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-form')")
    public ResponseEntity<BaseResponse<Void>> deleteForm(@PathVariable Long id) {
        customFormService.deleteForm(id);
        return ResponseEntity.ok(BaseResponse.ok( "Form deleted successfully"));
    }
}
