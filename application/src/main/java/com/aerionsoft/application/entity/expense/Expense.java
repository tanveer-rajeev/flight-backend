package com.aerionsoft.application.entity.expense;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.client.Ledger;
import com.aerionsoft.application.enums.expense.ExpenseStatus;
import com.aerionsoft.application.enums.common.UsingPortal;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expense")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_id", referencedColumnName = "id")
    private Ledger ledger;

    @Column(name = "expense_title", nullable = false)
    private String expenseTitle;

    @Column(name = "expense_description", length = 2000)
    private String expenseDescription;

    @Column(name = "expense_amount")
    private BigDecimal expenseAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_status")
    private ExpenseStatus expenseStatus;

    @Column(name = "expense_attachment")
    private String expenseAttachment;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @Enumerated(EnumType.STRING)
    @Column(name = "using_portal", nullable = false)
    private UsingPortal usingPortal;

    @Column(name = "portal_id")
    private Long portalId;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseDetail> expenseDetails = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
        if (expenseStatus == null) {
            expenseStatus = ExpenseStatus.PENDING;
        }
        if (expenseAmount == null) {
            expenseAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}

