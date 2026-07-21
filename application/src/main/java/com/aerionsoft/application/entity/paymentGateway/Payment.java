package com.aerionsoft.application.entity.paymentGateway;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.enums.wallet.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "payment_type")
    private PaymentType paymentType;

    // Customer info
    @Column(name = "customer_email")
    private String customerEmail;
    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "description")
    private String description;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // Basic card metadata (PCI safe)
    @Column(name = "card_brand")
    private String cardBrand;
    @Column(name = "card_holder_name")
    private String cardHolderName;
    @Column(name = "masked_pan")
    private String maskedPan;
    @Column(name = "card_expiry_month")
    private String cardExpiryMonth;

    @Column(name = "origin_ip")
    private String originIp;

    // N-Genius Specific
    @Column(name = "n_genius_payment_reference")
    private String ngeniusPaymentReference;
    @Column(name = "n_genius_order_reference")
    private String ngeniusOrderReference;
    @Column(name = "n_genius_payment_state")
    private String ngeniusPaymentState;
    @Column(name = "n_genius_update_date_time")
    private LocalDateTime ngeniusUpdateDateTime;
    @Column(name = "n_genius_outlet_id")
    private String ngeniusOutletId;
    @Column(name = "acs_trans_id")
    private String acsTransId;
    @Column(name = "authorization_code")
    private String authorizationCode;
    @Column(name = "retrieval_reference_number")
    private String retrievalReferenceNumber;
    @Column(name = "three_ds_trans_id")
    private String threeDsTransId;
    @Column(name = "electronic_commerce_indicator")
    private String electronicCommerceIndicator;
    @Column(name = "gateway_mid")
    private String gatewayMid;

    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;
    @Column(name = "stripe_created_at")
    private LocalDateTime stripeCreatedAt;

    @Column(name = "payment_method_id")
    private String paymentMethodId;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // requires_payment_method, requires_confirmation, requires_action, processing, requires_capture, canceled, succeeded

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at", nullable = false)
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
}
