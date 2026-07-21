package com.aerionsoft.application.dto.admin.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayDepositsSummaryResponse {
    /** Total approved deposit amount across all currencies */
    private Double todayDeposits;
    /** Total number of approved deposits */
    private Long todayApprovedDeposits;
    /** Per-currency breakdown (grouped by the depositing user's currency) */
    private List<DepositCurrencySummaryDto> byCurrency;
}

