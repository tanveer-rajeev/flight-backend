package com.aerionsoft.application.dto.client.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;

    // Constructor for backward compatibility
    public LoginResponse(String token) {
        this.token = token;
    }
}