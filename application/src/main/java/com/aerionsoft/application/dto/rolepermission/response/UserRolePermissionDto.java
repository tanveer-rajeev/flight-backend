package com.aerionsoft.application.dto.rolepermission.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRolePermissionDto {
    private Long userId;
    private String username;
    private String email;
    private RoleWithPermissionDto role;
    private List<PermissionResponseDto> permissions;
}

