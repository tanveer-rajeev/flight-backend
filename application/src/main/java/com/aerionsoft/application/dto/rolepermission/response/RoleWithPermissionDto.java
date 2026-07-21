package com.aerionsoft.application.dto.rolepermission.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoleWithPermissionDto {
    private Long id;
    private String name;
    private String slug;
    private Boolean isActive;
    private List<PermissionResponseDto> permissions;
}
