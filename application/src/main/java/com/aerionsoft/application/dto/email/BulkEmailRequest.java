package com.aerionsoft.application.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkEmailRequest {

    private List<String> toEmails;
    private List<String> ccEmails;
    private List<String> bccEmails;
    private String subject;
    private String body;
    private String templateName;
    private Map<String, String> templateVariables;
    private List<String> attachmentUrls;
    private Long businessId;
}