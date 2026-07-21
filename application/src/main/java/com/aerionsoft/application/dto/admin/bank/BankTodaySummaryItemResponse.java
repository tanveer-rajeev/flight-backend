package com.aerionsoft.application.dto.admin.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTodaySummaryItemResponse {
    private Long bankId;
    private String bankName;
    private String currency;
    private BigDecimal currentBalance;
    private BigDecimal todayDeposits;
    private BigDecimal todayWithdrawals;
    private BigDecimal todayNet;
}
