package com.aerionsoft.application.dto.visa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisaInfoRequest {
    private Long formId;
    private String country;
    private String visaType;
    private String description;
    private List<String> requiredDocuments;
    private String rules;
    private String processingTime;
    private BigDecimal feeAmount;
    private String currency;
    private String banner;
}
