package com.aerionsoft.application.controller.admin;


import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.subadmin.SubAdminDto;
import com.aerionsoft.application.dto.admin.subadmin.response.SubAdminResponseDto;
import com.aerionsoft.application.dto.client.user.UpdateProfileRequest;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.service.admin.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminUserController extends BaseController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping("/profile")
    public ResponseEntity<BaseResponse<UserDto>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserDto user = adminUserService.getProfile(email);
        return ResponseEntity.ok(BaseResponse.ok(user));
    }

    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-update-profile')")
    @PutMapping("/profile")
    public ResponseEntity<BaseResponse<UserDto>> updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest req) {
        Long authUserId = getUserIdFromAuthentication();

        return ResponseEntity.ok(BaseResponse.ok(adminUserService.updateProfile(authUserId, req)));
    }

    @PostMapping("/profile/image")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-add-profile-image')")
    public ResponseEntity<BaseResponse<String>> uploadProfileImage(Authentication authentication, @RequestParam("file") MultipartFile file) throws IOException {

        String email = authentication.getName();
        String filename = adminUserService.uploadProfileImage(email, file);
        String fileUri = "/uploads/admin/" + filename;
        return ResponseEntity.ok(BaseResponse.ok(fileUri));
    }

    @PostMapping("sub-admins")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-create-sub-admin')")
    public ResponseEntity<BaseResponse<SubAdminDto>> createSubAdminUser(@Valid @RequestBody SubAdminDto subAdminDto) {

        adminUserService.createSubAdminUser(subAdminDto);

        return ResponseEntity.ok(BaseResponse.ok("SubAdmin User created successfully"));
    }

    @GetMapping("sub-admins")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-view-sub-admin')")
    public ResponseEntity<BaseResponse<List<SubAdminResponseDto>>> getSubAdminUsers(@RequestParam(required = false) String currency, Authentication authentication) {
        List<SubAdminResponseDto> subAdminList = adminUserService.getSubAdminUsers(currency);

        return ResponseEntity.ok(BaseResponse.ok(subAdminList));
    }

    @DeleteMapping("sub-admins/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'admin-delete-sub-admin')")
    public ResponseEntity<BaseResponse<String>> removeSubAdminUser(@PathVariable Long id) {
        adminUserService.deleteSubAdminUser(id);

        return ResponseEntity.ok(BaseResponse.ok("SubAdmin User deleted successfully"));
    }
}
