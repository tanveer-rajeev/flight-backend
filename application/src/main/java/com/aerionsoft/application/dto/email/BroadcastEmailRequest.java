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
public class BroadcastEmailRequest {

    /** Send to specific user IDs (fetches their emails automatically). */
    private List<Long> toUserIds;

    /** Send to explicit email addresses directly. */
    private List<String> toEmails;

    /** If true, send to ALL active users (overrides toUserIds / toEmails). */
    private Boolean sendToAll;

    /** Optional: filter by user type when sendToAll = true (e.g. "AGENCY"). */
    private String userType;

    /** Email subject (required unless templateName is set). */
    private String subject;

    /** HTML email body (required unless templateName is set). */
    private String body;

    /** Name of a saved template to use instead of subject/body. */
    private String templateName;

    /** Variables to substitute in the template, e.g. {"name":"John"}. */
    private Map<String, String> templateVariables;

    /** Optional business scope (0 = global admin). */
    private Long businessId;
}

