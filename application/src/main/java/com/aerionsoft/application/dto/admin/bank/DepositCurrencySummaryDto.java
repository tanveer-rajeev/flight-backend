package com.aerionsoft.application.dto.admin.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositCurrencySummaryDto {
    private String currency;
    private Double totalAmount;
    private Long count;
}

