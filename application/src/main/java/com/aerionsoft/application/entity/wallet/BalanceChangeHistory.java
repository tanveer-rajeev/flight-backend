package com.aerionsoft.application.entity.wallet;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Audit log for every balance change on a user account.
 * One row is written for every call to addUserBalance / deductUserBalance.
 */
@Entity
@Table(name = "balance_change_history", indexes = {
        @Index(name = "idx_bch_user_id",   columnList = "user_id"),
        @Index(name = "idx_bch_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BalanceChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user whose wallet balance was changed. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** CREDIT = balance went up, DEBIT = balance went down. */
    @Column(name = "change_type", nullable = false, length = 10)
    private String changeType;          // "CREDIT" | "DEBIT"

    /** Absolute amount that was added / deducted. */
    @Column(name = "amount", nullable = false)
    private Double amount;

    /** Balance immediately before this change. */
    @Column(name = "balance_before", nullable = false)
    private Double balanceBefore;

    /** Balance immediately after this change. */
    @Column(name = "balance_after", nullable = false)
    private Double balanceAfter;

    /**
     * Human-readable reason: provider name for deductions
     * (e.g. "AMADEUS"), "REFUND", "DEPOSIT", "MANUAL", etc.
     */
    @Column(name = "reason", length = 255)
    private String reason;

    /**
     * The service/class that triggered the change,
     * e.g. "BookingService", "BookingCoordinatorService", "WalletService".
     */
    @Column(name = "source", length = 100)
    private String source;

    /** E.g. the booking ID or deposit ID related to this change. */
    @Column(name = "reference_id")
    private Long referenceId;

    /** "BOOKING", "DEPOSIT", "MANUAL", etc. */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * userId of the actor who triggered the operation
     * (admin, agent acting on behalf of another user, or the user themselves).
     * Null means triggered by the system automatically.
     */
    @Column(name = "performed_by")
    private Long performedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;
}
