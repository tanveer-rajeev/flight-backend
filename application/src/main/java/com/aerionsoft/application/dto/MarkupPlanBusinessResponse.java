package com.aerionsoft.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkupPlanBusinessResponse {
    private Long id;
    private Long markupPlanId;
    private String planTitle;
    private Long businessId;
    private String businessName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

