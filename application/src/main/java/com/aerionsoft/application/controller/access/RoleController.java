package com.aerionsoft.application.controller.access;

import com.aerionsoft.application.annotation.AuditedAction;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.rolepermission.AssignPermissionDto;
import com.aerionsoft.application.dto.rolepermission.AssignRoleDto;
import com.aerionsoft.application.dto.rolepermission.RoleDto;
import com.aerionsoft.application.dto.rolepermission.response.RoleResponseDto;
import com.aerionsoft.application.dto.rolepermission.response.RoleWithPermissionDto;
import com.aerionsoft.application.dto.rolepermission.response.UserRolePermissionDto;
import com.aerionsoft.application.service.access.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/roles")
public class RoleController extends BaseController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Get all roles
     *
     * @param page int
     * @param size int
     * @return response as JSON
     */
    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-role')")
    public ResponseEntity<BaseResponse<List<RoleResponseDto>>> getAllRoles(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        List<RoleResponseDto> roleList = roleService.getFilterRole(provider, authUserId, page, size);

        return ResponseEntity.ok(BaseResponse.ok(roleList));
    }

    /**
     * Get Role with permission
     *
     * @param roleSlug       find by slug
     * @param authentication authenticate user
     * @return response as JSON
     */
    @GetMapping("/{roleSlug}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-role')")
    public ResponseEntity<BaseResponse<RoleWithPermissionDto>> getRole(@PathVariable String roleSlug, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        RoleWithPermissionDto role = roleService.getRoleWithPermission(provider, authUserId, roleSlug);

        return ResponseEntity.ok(BaseResponse.ok(role));
    }

    /**
     * Create role
     *
     * @param roleDto        request data to create role
     * @param authentication authenticate user
     * @return response as JSON
     */
    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-role')")
    public ResponseEntity<BaseResponse<String>> createRole(@Valid @RequestBody RoleDto roleDto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        roleService.createRole(provider, authUserId, roleDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created("Role created successfully", null));
    }

    /**
     * Assign permissions to a role
     *
     * @param assignPermissionDto request to set permissions o role
     * @param authentication      authenticate user
     * @return response JSON
     */
    @PostMapping("assign-permissions")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'assign-permission-to-role')")
    @AuditedAction(value = ActivityEventType.PERMISSION_ASSIGNED, resourceType = "ROLE")
    public ResponseEntity<BaseResponse<List<AssignPermissionDto>>> assignPermissionsToRole(@Valid @RequestBody AssignPermissionDto assignPermissionDto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        roleService.assignPermissionsToRole(provider, authUserId, assignPermissionDto);

        return ResponseEntity.ok(BaseResponse.ok("Permissions assigned successfully"));
    }

    /**
     * Assign role to a user
     *
     * @param assignRoleDto  request data to set role to a user
     * @param authentication authenticate user
     * @return response as JSON
     */
    @PostMapping("assign-to-users")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'assign-role-to-user')")
    @AuditedAction(value = ActivityEventType.ROLE_ASSIGNED, resourceType = "USER")
    public ResponseEntity<BaseResponse<AssignRoleDto>> assignRolesToUser(@Valid @RequestBody AssignRoleDto assignRoleDto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        roleService.assignRoleToUser(provider, authUserId, assignRoleDto);

        return ResponseEntity.ok(BaseResponse.ok("Roles assigned successfully"));
    }

    /**
     * My role with permission
     *
     * @param authentication authenticate user
     * @return response as JSON
     */
    @GetMapping("/my-role")
    public ResponseEntity<BaseResponse<RoleWithPermissionDto>> myRoleWithPermission(Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        RoleWithPermissionDto role = roleService.myRoleWithPermissions(provider, authUserId);

        return ResponseEntity.ok(BaseResponse.ok(role));
    }

    /**
     * Get user's role and permissions by user ID
     *
     * @param userId User ID
     * @return response as JSON with user's role and permissions
     */
    @GetMapping("/user/{userId}/permissions")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-permission')")
    public ResponseEntity<BaseResponse<UserRolePermissionDto>> getUserRoleAndPermissions(
            @PathVariable Long userId) {
        UserRolePermissionDto userRolePermissions = roleService.getUserRoleAndPermissions(userId);

        return ResponseEntity.ok(BaseResponse.ok(userRolePermissions));
    }
}
