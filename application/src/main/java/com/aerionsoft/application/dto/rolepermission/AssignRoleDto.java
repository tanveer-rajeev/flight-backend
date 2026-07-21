package com.aerionsoft.application.dto.rolepermission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleDto {

    @NotNull(message = "Id is required")
    private Long id;

    @NotBlank(message = "Entity type must be either ADMIN or USER")
    private String entity;

    @NotEmpty(message = "Role slug is required")
    private String role;
}
