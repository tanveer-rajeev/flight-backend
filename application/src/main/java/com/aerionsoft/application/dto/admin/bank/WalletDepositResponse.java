package com.aerionsoft.application.dto.admin.bank;

import com.aerionsoft.application.entity.wallet.DepositBank;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletDepositResponse {
    private Long id;
    private String reference;
    private DepositType type;
    private DepositStatus status;
    private Double amount;
    private Double exchangedAmount;
    private Double exchangeRate;
    private String remarks;
    private String attachment;
    private String chequeNo;
    private String chequeBank;
    private String chequeIssueDate;
    private DepositBank depositBank;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String createdBy;
    private String approvedBy;
    private String clientSecret;
    private String paymentIntentId;
    private String transactionId;
    private String createdByName;
    private Currency currency;
    // Agency info
    private Long agencyId;
    private String agencyName;
    private LocalDate depositDate;
}