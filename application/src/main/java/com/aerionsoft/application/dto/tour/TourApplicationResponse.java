package com.aerionsoft.application.dto.tour;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TourApplicationResponse {
    private Long id;
    private Long formId;
    private Long tourId;
    private Map<String, Object> formResponses;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
    private String processedBy;
    private String remarks;
    private String createdBy;
    private String createdByName;
}

