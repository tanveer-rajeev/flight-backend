package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.AdminResetPasswordRequest;
import com.aerionsoft.application.dto.admin.ChangePasswordRequest;
import com.aerionsoft.application.dto.client.user.CreateUserRequest;
import com.aerionsoft.application.dto.client.user.UpdateUserRequest;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.service.user.UserCoordinatorService;
import com.aerionsoft.application.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/admin/user-agency")
@PreAuthorize("hasRole('ADMIN')")
public class UsersController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserCoordinatorService  coordinatorService;

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-user')")
    public ResponseEntity<BaseResponse<Page<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean isAgency,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdDate
    ) {
        Page<UserDto> users = userService.getFilteredUser(page, size, query, isAgency, status, createdDate);
        return ResponseEntity.ok(BaseResponse.ok("Users retrieved successfully", users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-user')")
    public ResponseEntity<BaseResponse<UserDto>> getUserById(@PathVariable Long id, @RequestParam Boolean isAgency) {
        UserDto user = coordinatorService.getAgentInfo(id,isAgency);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BaseResponse.ok("User retrieved successfully", user));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-user')")
    public ResponseEntity<BaseResponse<Long>> createUser(@Valid @RequestBody CreateUserRequest request) {
            Long userId = userService.createUser(request,false);
            return ResponseEntity.ok(BaseResponse.ok("User created successfully", userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-user')")
    public ResponseEntity<BaseResponse<String>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
            userService.updateUser(id, request);
            return ResponseEntity.ok(BaseResponse.ok("User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-user')")
    public ResponseEntity<BaseResponse<String>> deleteUser(@PathVariable Long id) {
            userService.deleteUser(id);
            return ResponseEntity.ok(BaseResponse.ok("User deleted successfully"));
    }

    /**
     * Delete an agency account (soft-delete). Rejected if any {@code transactions} row exists for
     * the agency user or its child users.
     */
    @DeleteMapping("/agencies/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-user')")
    public ResponseEntity<BaseResponse<String>> deleteAgency(@PathVariable Long id) {
            userService.deleteAgency(id);
            return ResponseEntity.ok(BaseResponse.ok("Agency deleted successfully"));
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'change-user-password')")
    public ResponseEntity<BaseResponse<String>> changeUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
            UserDto user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            userService.changePassword(user.getEmail(), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(BaseResponse.ok("Password changed successfully"));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'change-user-password')")
    public ResponseEntity<BaseResponse<String>> resetUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
            UserDto user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            userService.resetPasswordByAdmin(id, request.getNewPassword());
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .success(true)
                    .message("Password reset successfully")
                    .status(200)
                    .data("Password reset successfully")
                    .build());
    }

}
