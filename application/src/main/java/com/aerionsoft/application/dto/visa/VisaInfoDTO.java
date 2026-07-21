package com.aerionsoft.application.dto.visa;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaInfoDTO {
    private String country;
    private String visaType;
    private String description;
    private List<String> requiredDocuments;
    private String rules;
    private String processingTime;
    private BigDecimal feeAmount;
    private String currency;
}
