package com.aerionsoft.application.entity.wallet;

import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.entity.HasCreatedUserTimestamp;
import com.aerionsoft.application.entity.listener.UserTimestampListener;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(UserTimestampListener.class)
@org.hibernate.annotations.BatchSize(size = 50)
public class WalletDeposit implements HasCreatedUserTimestamp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;

    // Column to track if another user (child) is acting on behalf of this user (parent)
    @Column(name = "acting_user_id")
    private Long actingUserId;

    @Enumerated(EnumType.STRING)
    private DepositType type;

    @Enumerated(EnumType.STRING)
    private DepositStatus status;

    private Double amount;
    private Double exchangedAmount; // amount after exchange if applicable
    private Double exchangeRate;

    @Column(unique = true)
    private String reference;   // auto-generated

    private String remarks;
    private String attachment;  // file path/url

    // Cheque fields
    private String chequeNo;
    private String chequeBank;
    private String chequeIssueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private DepositBank depositBank;

    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    private LocalDateTime approvedAt;
    private Long approvedBy;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    private Currency currency;

    private LocalDate depositDate;

}
