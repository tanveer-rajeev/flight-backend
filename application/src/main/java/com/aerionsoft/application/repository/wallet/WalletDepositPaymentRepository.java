package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.wallet.WalletDepositPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletDepositPaymentRepository extends JpaRepository<WalletDepositPayment, Long> {
    Optional<WalletDepositPayment> findByTransactionId(String trxId);
}