package com.aerionsoft.application.dto.admin.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankStatementResponse {
    private Long bankId;
    private String bankName;
    private String currency;
    private LocalDate from;
    private LocalDate to;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private BigDecimal todayDeposits;
    private BigDecimal todayWithdrawals;
    private List<BankLedgerEntryResponse> entries;
}
