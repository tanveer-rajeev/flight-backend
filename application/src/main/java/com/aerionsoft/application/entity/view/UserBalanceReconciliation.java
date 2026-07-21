package com.aerionsoft.application.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * Read-only entity mapped to the PostgreSQL view {@code user_balance_reconciliation}.
 * <p>
 * Wallet owners only (no child users). Transaction totals use {@code is_active = true}
 * rows and roll child {@code user_id} activity up to the mother wallet user.
 */
@Getter
@NoArgsConstructor
@Immutable
@Entity
@Table(name = "user_balance_reconciliation")
public class UserBalanceReconciliation {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "agency_name")
    private String agencyName;

    @Column(name = "total_credit")
    private BigDecimal totalCredit;

    @Column(name = "total_debit")
    private BigDecimal totalDebit;

    @Column(name = "deb_cred_balance")
    private BigDecimal debCredBalance;

    @Column(name = "user_balance")
    private BigDecimal userBalance;

    @Column(name = "diff_amount")
    private BigDecimal diffAmount;
}

