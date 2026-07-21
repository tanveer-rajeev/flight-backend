package com.aerionsoft.application.controller.finance;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.accounthead.AccountHeadRequest;
import com.aerionsoft.application.dto.accounthead.AccountHeadResponse;
import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.enums.common.UsingPortal;
import com.aerionsoft.application.service.finance.AccountHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/account-head")
public class AccountHeadController extends  BaseController {

    @Autowired
    private AccountHeadService accountHeadService;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-account-head')") // admin and user
    public ResponseEntity<BaseResponse<AccountHeadResponse>> createAccountHead(
            @Valid @RequestBody AccountHeadRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication();
        AccountHeadResponse response = accountHeadService.createAccountHead(request, userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Account Head created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-account-head')") // admin and user
    public ResponseEntity<BaseResponse<AccountHeadResponse>> getAccountHeadById(@PathVariable Long id) {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        AccountHeadResponse response = accountHeadService.getAccountHeadById(id, userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(response, "Account Head retrieved successfully"));
    }

    @GetMapping("/all")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-account-head')") // admin and user
    public ResponseEntity<BaseResponse<List<AccountHeadResponse>>> getAllAccountHeads() {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        List<AccountHeadResponse> response = accountHeadService.getAllAccountHeads(userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(response, "Account Heads retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-account-head')") // admin and user
    public ResponseEntity<BaseResponse<Page<AccountHeadResponse>>> getAllAccountHeadsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Long userId = getUserIdFromAuthentication();
        boolean isAdmin = isAdmin();
        Page<AccountHeadResponse> response = accountHeadService.getAllAccountHeadsPaginated(page, size, sortBy, sortDir, userId, isAdmin);
        return ResponseEntity.ok(BaseResponse.ok(response, "Account Heads retrieved successfully with pagination"));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-account-head')") // admin and user
    public ResponseEntity<BaseResponse<List<AccountHeadResponse>>> getAccountHeadsByType(
            @PathVariable AccountHeadType type) {
        List<AccountHeadResponse> response = accountHeadService.getAccountHeadsByType(type);
        return ResponseEntity.ok(BaseResponse.ok(response, "Account Heads by type retrieved successfully"));
    }

    @GetMapping("/parent/{parentId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-account-head')") // admin and user
    public ResponseEntity<BaseResponse<List<AccountHeadResponse>>> getAccountHeadsByParentId(
            @PathVariable Long parentId) {
        List<AccountHeadResponse> response = accountHeadService.getAccountHeadsByParentId(parentId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Child Account Heads retrieved successfully"));
    }

    @GetMapping("/portal/{portal}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-account-head')") // admin and user
    public ResponseEntity<BaseResponse<List<AccountHeadResponse>>> getAccountHeadsByPortal(
            @PathVariable UsingPortal portal) {
        List<AccountHeadResponse> response = accountHeadService.getAccountHeadsByPortal(portal);
        return ResponseEntity.ok(BaseResponse.ok(response, "Account Heads by portal retrieved successfully"));
    }

    @GetMapping("/portal/{portal}/{portalId}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-account-head')") // admin and user
    public ResponseEntity<BaseResponse<List<AccountHeadResponse>>> getAccountHeadsByPortalAndId(
            @PathVariable UsingPortal portal,
            @PathVariable Long portalId) {
        List<AccountHeadResponse> response = accountHeadService.getAccountHeadsByPortalAndId(portal, portalId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Account Heads by portal and ID retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-account-head')") // admin and user
    public ResponseEntity<BaseResponse<AccountHeadResponse>> updateAccountHead(
            @PathVariable Long id,
            @Valid @RequestBody AccountHeadRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication();
        AccountHeadResponse response = accountHeadService.updateAccountHead(id, request, userId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Account Head updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-account-head')")
    public ResponseEntity<BaseResponse<Void>> deleteAccountHead(@PathVariable Long id) {
        accountHeadService.deleteAccountHead(id);
        return ResponseEntity.ok(BaseResponse.ok("Account Head deleted successfully"));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        // This is a placeholder - adjust based on your actual authentication implementation
        // You might need to cast to your custom UserDetails implementation
        return 1L; // Replace with actual user ID extraction logic
    }
}
