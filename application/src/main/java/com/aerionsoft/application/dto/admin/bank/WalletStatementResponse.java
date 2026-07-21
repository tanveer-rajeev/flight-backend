package com.aerionsoft.application.dto.admin.bank;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletStatementResponse {
    private LocalDateTime date;
    private String transactionType; // e.g. DEPOSIT, WITHDRAWAL, REFUND
    private String transactionCode; // e.g. DB/WD/RF/etc (optional for your mapping)
    private String referenceNumber;
    private String notes;
    private Double withdrawal; // amount if this is withdrawal, else null/0
    private Double deposit;    // amount if this is deposit/refund, else null/0
    private Double balance;    // running balance after this txn
}
