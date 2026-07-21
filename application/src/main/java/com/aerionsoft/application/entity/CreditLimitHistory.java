package com.aerionsoft.application.entity;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_limit_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLimitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "cause", columnDefinition = "TEXT")
    private String cause;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @Column(name = "admin_instruction", columnDefinition = "TEXT")
    private String adminInstruction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CreditLimitStatus status;

    @Column(name = "balance_before", precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

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

