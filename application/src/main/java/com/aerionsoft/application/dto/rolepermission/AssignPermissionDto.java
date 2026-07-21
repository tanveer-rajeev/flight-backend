package com.aerionsoft.application.dto.rolepermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssignPermissionDto {

    @NotBlank(message = "Role slug is required")
    private String slug;

    @NotEmpty(message = "At least one permission slug is required")
    private List<String> permissions;
}
