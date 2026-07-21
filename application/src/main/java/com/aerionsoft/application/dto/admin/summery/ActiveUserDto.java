package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveUserDto {
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String userType; // "USER" or "ADMIN"
    private String lastLoginAt;
    private String ipAddress;
    private String userAgent;
    private boolean isAgency;
}

