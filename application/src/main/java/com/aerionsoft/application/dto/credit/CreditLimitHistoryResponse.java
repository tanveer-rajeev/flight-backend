package com.aerionsoft.application.dto.credit;

import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
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
public class CreditLimitHistoryResponse {

    private Long id;

    private Long businessId;

    private String businessName;

    private BigDecimal amount;

    private String cause;

    private LocalDateTime returnDate;

    private Long createdBy;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String adminInstruction;

    private CreditLimitStatus status;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;
}

