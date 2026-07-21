package com.aerionsoft.application.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginHistoryDto {

    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginTime;
}
