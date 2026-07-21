package com.aerionsoft.application.entity.wallet;

import com.aerionsoft.application.enums.wallet.BankEntryChannel;
import com.aerionsoft.application.enums.wallet.BankLedgerCategory;
import com.aerionsoft.application.enums.wallet.BankLedgerEntryType;
import com.aerionsoft.application.enums.wallet.BankLedgerSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_id", nullable = false)
    private DepositBank bank;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private BankLedgerEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BankLedgerCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_channel", nullable = false, length = 16)
    private BankEntryChannel entryChannel;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private BankLedgerSourceType sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(length = 64)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;
}
