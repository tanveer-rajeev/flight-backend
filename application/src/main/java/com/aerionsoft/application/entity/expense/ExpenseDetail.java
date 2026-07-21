package com.aerionsoft.application.entity.expense;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.enums.common.UsingPortal;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", insertable = false, updatable = false)
    private Expense expense;

    @Column(name = "account_head_id", nullable = false)
    private Long accountHeadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_head_id", insertable = false, updatable = false)
    private AccountHead accountHead;

    @Column(name = "item_title", nullable = false)
    private String itemTitle;

    @Column(name = "item_description", length = 2000)
    private String itemDescription;

    @Column(name = "item_amount", nullable = false)
    private BigDecimal itemAmount;

    @Column(name = "item_attachment")
    private String itemAttachment;

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

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}
