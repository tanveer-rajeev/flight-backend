package com.aerionsoft.application.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCredentialsRequest {

    private String smtpHost;
    private Integer smtpPort;
    private String username;
    private String password;
    private String fromEmail;
    private String fromName;
    private Boolean isSslEnabled;
    private Long businessId;
}