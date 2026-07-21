package com.aerionsoft.application.dto.admin.bank;

import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.DepositType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DepositRequest {
    @NotNull
    private DepositType type;
    @NotNull private Double amount;
    private Double exchangedAmount;
    private Double exchangeRate;
    private String remarks;
    private Long depositBankId;
    private String chequeNo;
    private String chequeBank;
    private String chequeIssueDate;
    private String transactionId; // for online deposits
    @NotNull(message = "Currency is required")
    private Currency currency;
    @NotBlank(message = "reference is reqiored")
    private String reference;
    private LocalDate depositDate;
}