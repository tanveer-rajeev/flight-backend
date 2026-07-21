package com.aerionsoft.application.repository.payment;

import com.aerionsoft.application.entity.paymentGateway.SslCommerzPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SslCommerzPaymentRepository extends JpaRepository<SslCommerzPayment, Long> {
    List<SslCommerzPayment> findByBookingId(Long bookingId);

    Optional<SslCommerzPayment> findByTranId(String tranId);
    
    // Wallet deposit specific queries
    Optional<SslCommerzPayment> findByDepositReference(String depositReference);
    List<SslCommerzPayment> findByPaymentType(SslCommerzPayment.PaymentType paymentType);
    List<SslCommerzPayment> findByUserIdAndPaymentType(Long userId, SslCommerzPayment.PaymentType paymentType);
    List<SslCommerzPayment> findByUserIdAndStatus(Long userId, String status);
}