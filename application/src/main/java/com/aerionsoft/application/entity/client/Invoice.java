package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.Booking.Traveller;
import com.aerionsoft.application.enums.client.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "agency_id",nullable = true)
    private User agencyUser;

    @ManyToOne()
    @JoinColumn(name = "traveler_id", nullable = true, referencedColumnName = "id",
                foreignKey = @ForeignKey(name = "fk_invoice_traveller"))
    private Traveller traveller;

    @ManyToOne()
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @Column(nullable = false, length = 100)
    private String invoiceTitle;

    @Column(name = "invoice_details", columnDefinition = "TEXT")
    private String invoiceDetails;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "invoice_amount", precision = 14, scale = 4, nullable = false)
    private BigDecimal invoiceAmount;

    @Column(name = "invoice_revenue", precision = 14, scale = 4, nullable = false)
    private BigDecimal invoiceRevenue;

    @Column(name = "invoice_service_charge",  precision = 10, scale = 4)
    private BigDecimal invoiceServiceCharge;

    @Column(name = "invoice_discount", precision = 10, scale = 4)
    private BigDecimal invoiceDiscount;

    @Column(name = "payment_method", length = 100, nullable = false)
    private String paymentMethod;

    @Column
    private String document;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItem> invoiceItems = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.status = InvoiceStatus.PENDING;
        this.createdAt = UserDateTimeUtil.now();
        this.invoiceRevenue = BigDecimal.ZERO;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = UserDateTimeUtil.now();
    }
}
