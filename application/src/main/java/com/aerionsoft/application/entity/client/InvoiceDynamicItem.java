package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_dynamic_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceDynamicItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_item_id", nullable = false)
    private InvoiceItem invoiceItem;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @PrePersist
    public void onCreate() {
        this.createAt = UserDateTimeUtil.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updateAt = UserDateTimeUtil.now();
    }
}
