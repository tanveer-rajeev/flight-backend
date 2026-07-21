package com.aerionsoft.application.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResponse {

    private Long id;
    private String toEmail;
    private String subject;
    private String status;
    private String errorMessage;
}
