package com.aerionsoft.application.controller.access;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.rolepermission.PermissionGroupDto;
import com.aerionsoft.application.dto.rolepermission.response.PermissionGroupResponseDto;
import com.aerionsoft.application.service.access.PermissionGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/permission-groups")
public class PermissionGroupController extends BaseController {
    @Autowired
    private PermissionGroupService permissionGroupService;

    @PostMapping
    public ResponseEntity<BaseResponse<?>> createPermissionGroup(@Valid @RequestBody PermissionGroupDto permissionGroupDto) {
        permissionGroupService.createPermissionGroup(permissionGroupDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created("Permission group created successfully", null));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<PermissionGroupResponseDto>>>  getPermissionGroups() {
        List<PermissionGroupResponseDto> permissionGroup = permissionGroupService.getPermissionGroups();

        return ResponseEntity.ok(BaseResponse.ok(permissionGroup));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<?>> updatePermissionGroup(@PathVariable Long id, @Valid @RequestBody PermissionGroupDto permissionGroupDto) {
        permissionGroupService.updatePermissionGroup(id, permissionGroupDto);

        return ResponseEntity.ok(BaseResponse.ok("Permission group updated successfully"));
    }
}

