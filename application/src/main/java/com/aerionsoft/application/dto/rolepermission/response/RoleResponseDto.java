package com.aerionsoft.application.dto.rolepermission.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleResponseDto {
    private Long id;
    private String name;
    private String slug;
    private Boolean isActive;
}
