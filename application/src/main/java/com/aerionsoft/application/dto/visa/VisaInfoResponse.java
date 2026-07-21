package com.aerionsoft.application.dto.visa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisaInfoResponse {
    private Long id;
    private Long formId;
    private String country;
    private String visaType;
    private String description;
    private List<String> requiredDocuments;
    private String rules;
    private String processingTime;
    private BigDecimal feeAmount;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String banner;
}
