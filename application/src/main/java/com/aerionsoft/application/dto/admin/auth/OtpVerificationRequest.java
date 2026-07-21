package com.aerionsoft.application.dto.admin.auth;

import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String email;
    private String otp;
}
