package com.aerionsoft.application.dto.admin.bank;

import com.aerionsoft.application.enums.wallet.BankEntryChannel;
import com.aerionsoft.application.enums.wallet.BankLedgerCategory;
import com.aerionsoft.application.enums.wallet.BankLedgerEntryType;
import com.aerionsoft.application.enums.wallet.BankLedgerSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankLedgerEntryResponse {
    private Long id;
    private LocalDateTime date;
    private BankLedgerEntryType entryType;
    private BankLedgerCategory category;
    private BankEntryChannel entryChannel;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private BankLedgerSourceType sourceType;
    private Long sourceId;
    private String reference;
    private String description;
    private String createdBy;
}
