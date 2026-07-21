package com.aerionsoft.application.repository.payment;

import com.aerionsoft.application.entity.paymentGateway.TabbyPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TabbyPaymentRepository extends JpaRepository<TabbyPayment,Long> {
     Optional<TabbyPayment> findByOrderId(String orderId);
     Optional<TabbyPayment> findByPaymentId(String tabbyPaymentId);
}
