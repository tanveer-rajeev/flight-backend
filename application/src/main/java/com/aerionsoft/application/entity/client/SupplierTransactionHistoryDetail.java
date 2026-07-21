package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_transaction_history_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierTransactionHistoryDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_transaction_history_id", nullable = false)
    private SupplierTransactionHistory supplierTransactionHistory;

    @Column(name = "detail_key", nullable = false)
    private String key;

    @Column(name = "detail_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @PrePersist
    public void onCreate() {
        this.createdAt = UserDateTimeUtil.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = UserDateTimeUtil.now();
    }
}

