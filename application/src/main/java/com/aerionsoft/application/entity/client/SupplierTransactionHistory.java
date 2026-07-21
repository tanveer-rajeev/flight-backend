package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.enums.client.ManualInvoicePaymentType;
import com.aerionsoft.application.entity.wallet.DepositBank;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier_transaction_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierTransactionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "invoice_item_id")
    private Long invoiceItemId;

    @Column(name = "agency_id")
    private Long agencyId;

    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "payable_amount", precision = 19, scale = 4)
    private BigDecimal payableAmount;

    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount;

    @Column(name = "attachments")
    private String attachments;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "title")
    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdDate;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "payment_type")
    private ManualInvoicePaymentType paymentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_bank_id")
    private DepositBank depositBank;

    @OneToMany(mappedBy = "supplierTransactionHistory", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SupplierTransactionHistoryDetail> details = new ArrayList<>();
}
