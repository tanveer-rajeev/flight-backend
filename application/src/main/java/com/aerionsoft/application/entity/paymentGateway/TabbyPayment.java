package com.aerionsoft.application.entity.paymentGateway;

import com.aerionsoft.application.audit.BaseAuditEntity;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.payment.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tabby_payments")
public class TabbyPayment extends BaseAuditEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Version
    private Long version;
}
