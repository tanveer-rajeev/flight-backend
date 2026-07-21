package com.aerionsoft.application.controller.access;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.rolepermission.PermissionDto;
import com.aerionsoft.application.dto.rolepermission.response.PermissionResponseDto;
import com.aerionsoft.application.service.access.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("api/permissions")
public class PermissionController extends BaseController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-permission')")
    public ResponseEntity<BaseResponse<Map<String, List<PermissionResponseDto>>>> getAllPermissions(
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "permissionGroupId", required = false) Long permissionGroupId) {
        Map<String, List<PermissionResponseDto>> permissionList = permissionService.getAllPermissions(module, type, permissionGroupId);

        return ResponseEntity.ok(BaseResponse.ok(permissionList));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-permission')")
    public ResponseEntity<BaseResponse<String>> addPermission(@Valid @RequestBody PermissionDto permissionDto) {
        permissionService.createPermission(permissionDto);

        return ResponseEntity.ok(BaseResponse.ok("Permission created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionDto permissionDto) {
        permissionService.updatePermission(id, permissionDto);

        return ResponseEntity.ok(BaseResponse.ok("Permission updated successfully"));
    }

    @GetMapping("/menus")
    ResponseEntity<BaseResponse<List<PermissionResponseDto>>> getAllMenus(Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        List<PermissionResponseDto> permissionList = permissionService.getMenuPermissions(provider, authUserId);

        return ResponseEntity.ok(BaseResponse.ok(permissionList));
    }
}
