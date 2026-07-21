package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierTransactionReportSummaryDTO {
    private BigDecimal totalSell;
    private BigDecimal totalPurchase;
    private BigDecimal totalDeposit;
    private BigDecimal totalProfitLoss;
    private BigDecimal outstandingBalance;
}
