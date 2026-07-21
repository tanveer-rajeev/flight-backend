package com.aerionsoft.application.dto.rolepermission;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionGroupDto {
    @NotBlank(message = "Name is required")
    private String name;
}
