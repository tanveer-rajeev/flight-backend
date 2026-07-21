package com.aerionsoft.application.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastEmailResponse {

    private int totalRecipients;
    private int successCount;
    private int failureCount;
    private List<String> failedEmails;
    private String message;
}

