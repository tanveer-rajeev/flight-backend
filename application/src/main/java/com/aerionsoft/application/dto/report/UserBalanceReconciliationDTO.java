package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBalanceReconciliationDTO {

    private Long userId;
    private String agencyName;
    private BigDecimal totalCredit;
    private BigDecimal totalDebit;
    private BigDecimal debCredBalance;
    private BigDecimal userBalance;
    private BigDecimal diffAmount;
}

