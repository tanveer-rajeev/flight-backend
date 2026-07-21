package com.aerionsoft.application.entity.wallet;

import com.aerionsoft.application.enums.wallet.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletDepositPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "deposit_id", nullable = false)
    private WalletDeposit deposit;   // link to deposit

    @Enumerated(EnumType.STRING)
    private PaymentProvider method;    // STRIPE, BKASH, NAGAD, BANK_TRANSFER, CASH, CHEQUE

    private String transactionId;    // e.g. Stripe PI id, bKash trx id
    private String referenceNo;      // bank ref, cheque no, etc.
    private String currency;         // USD, BDT, etc.
    private Double amount;           // paid amount
    private String status;           // pending, succeeded, failed

    private LocalDateTime paidAt;
    private LocalDateTime confirmedAt;

    @Lob
    private String rawResponse;      // store JSON from gateway if needed
}