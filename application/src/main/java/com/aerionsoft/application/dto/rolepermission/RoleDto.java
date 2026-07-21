package com.aerionsoft.application.dto.rolepermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleDto {

    @NotBlank(message = "Role name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    @Pattern(
            regexp = "^[a-z0-9_-]+$",
            message = "Slug can only contain lowercase letters, numbers, hyphens, and underscores"
    )
    private String slug;

    private Boolean isActive = true;

    private Long agencyId;
}
