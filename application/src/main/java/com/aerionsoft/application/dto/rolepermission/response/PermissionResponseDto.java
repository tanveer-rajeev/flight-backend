package com.aerionsoft.application.dto.rolepermission.response;

import com.aerionsoft.application.entity.rolePermission.PermissionGroup;
import com.aerionsoft.application.enums.access.PermissionModule;
import com.aerionsoft.application.enums.access.PermissionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionResponseDto {
    private Long id;
    private String name;
    private String slug;
    private PermissionType type;
    private PermissionModule module;
    private PermissionGroup permissionGroup;
    private Boolean isActive;
}
