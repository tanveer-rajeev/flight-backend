package com.aerionsoft.application.entity.wallet;

import com.aerionsoft.application.entity.HasCreatedUserTimestamp;
import com.aerionsoft.application.entity.HasUpdatedUserTimestamp;
import com.aerionsoft.application.entity.listener.UserTimestampListener;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(UserTimestampListener.class)
public class Transaction implements HasCreatedUserTimestamp, HasUpdatedUserTimestamp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    private String type;
    private Double amount;
    private String currency;
    private String convertedAmount;

    @Column(name = "converted_currency")
    private String convertedCurrency;

    @Column(name = "converted_rate")
    private String convertedRate;

    private Double exchangeRate;
    private String description;
    private Long userId;
    private String createdBy;
    private String updatedBy;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    /** Wallet deposit business reference (e.g. FR…, dp…) — not a polymorphic entity id. */
    private String reference;

    @Column(name = "status")
    @Builder.Default
    private Boolean status = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public void linkSource(TransactionSourceType type, Long id) {
        this.sourceType = type != null ? type.name() : null;
        this.sourceId = id;
    }
}
