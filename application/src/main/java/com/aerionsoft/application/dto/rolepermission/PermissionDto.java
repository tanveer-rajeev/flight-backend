package com.aerionsoft.application.dto.rolepermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionDto {

    @NotBlank(message = "Permission name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 100)
    @Pattern(
            regexp = "^[a-z0-9_-]+$",
            message = "Slug can only contain lowercase letters, numbers, hyphens, and underscores"
    )
    private String slug;

    @NotBlank(message = "Type is required in (ADMIN, AGENCY, USER, GLOBAL)")
    private String type;

    @NotBlank(message = "Module is required in (API, MENU, CUSTOM)")
    private String module;

    @NotNull(message = "Permission group id is required")
    private Long permissionGroupId;

    private Boolean isActive = true;
}
