package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.enums.client.InvoiceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private User agencyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_head_id", nullable = false)
    private AccountHead accountHead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(length = 100)
    private String title;

    @Column
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceType invoiceType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "sell_price", precision = 10, scale = 4, nullable = false)
    private BigDecimal sellPrice;

    @Column(name = "buy_price", precision = 10, scale = 4, nullable = false)
    private BigDecimal buyPrice;

    @Column(nullable = false)
    private Integer step;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

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
