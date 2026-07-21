package com.aerionsoft.application.entity.paymentGateway;

import com.aerionsoft.application.enums.wallet.PaymentProvider;
import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ssl_commerz_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SslCommerzPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "deposit_reference")
    private String depositReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType = PaymentType.BOOKING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider method;

    @Column(name = "tran_id", nullable = false, unique = true)
    private String tranId;

    @Column(name = "session_key")
    private String sessionKey;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // INITIATED, PENDING, VALIDATED, FAILED, CANCELLED

    @Column(name = "validation_id")
    private String validationId;

    @Column(name = "bank_tran_id")
    private String bankTranId;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "card_no")
    private String cardNo;

    @Column(name = "card_issuer")
    private String cardIssuer;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "card_issuer_country")
    private String cardIssuerCountry;

    @Column(name = "store_amount", precision = 10, scale = 2)
    private BigDecimal storeAmount;

    @Column(name = "verify_sign")
    private String verifySign;

    @Column(name = "verify_sign_sha2")
    private String verifySignSha2;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "risk_title")
    private String riskTitle;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }

    // Enum for payment types
    public enum PaymentType {
        BOOKING, WALLET_DEPOSIT,SUCCESS
    }
}