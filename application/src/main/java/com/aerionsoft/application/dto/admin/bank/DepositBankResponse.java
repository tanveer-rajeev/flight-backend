package com.aerionsoft.application.dto.admin.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepositBankResponse {
    private Long id;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String routingNumber;
    private String branch;
    private String currency;
    private Boolean isActive;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
}

