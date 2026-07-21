package com.aerionsoft.application.dto.admin.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}