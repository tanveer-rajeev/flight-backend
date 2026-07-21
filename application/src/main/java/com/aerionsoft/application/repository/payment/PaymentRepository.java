package com.aerionsoft.application.repository.payment;

import com.aerionsoft.application.entity.paymentGateway.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Payment> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);

    List<Payment> findByStatus(String status);

    Optional<Payment> findByBookingIdAndStatus(Long bookingId, String status);

    Optional<Payment> findByNgeniusOrderReference(String reference);
}
