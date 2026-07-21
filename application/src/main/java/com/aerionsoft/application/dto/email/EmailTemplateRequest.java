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
public class EmailTemplateRequest {

    private String templateName;
    private String subject;
    private String body;
    private String templateType;
    private List<String> variables;
    private Long businessId;
}
