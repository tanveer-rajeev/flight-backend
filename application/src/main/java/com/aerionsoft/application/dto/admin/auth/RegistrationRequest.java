package com.aerionsoft.application.dto.admin.auth;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String email;
    private String password;
    private String fullName;
}