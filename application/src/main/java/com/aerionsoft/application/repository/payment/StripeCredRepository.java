package com.aerionsoft.application.repository.payment;

import com.aerionsoft.application.entity.paymentGateway.StripeCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeCredRepository extends JpaRepository<StripeCredentials, Long> {

}