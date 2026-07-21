package com.aerionsoft.application.dto.credit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditRequestDto {

    private Long id;

    private Long businessId;

    private String businessName;

    private BigDecimal requestedAmount;

    private String reason;

    private String requestStatus; // PENDING, APPROVED, REJECTED

    private Long requestedBy;

    private String requestedByName;

    private LocalDateTime requestedAt;

    private Long processedBy;

    private String processedByName;

    private LocalDateTime processedAt;

    private String adminRemarks;

    private BigDecimal approvedAmount;
}

