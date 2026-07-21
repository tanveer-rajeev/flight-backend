package com.aerionsoft.application.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImpersonateUserRequest {

    @NotNull
    private Long userId;

    @Size(max = 200)
    private String reason;
}

