package com.aerionsoft.application.repository.gateway;

import com.aerionsoft.application.entity.paymentGateway.NGeniusCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NGeniusCredentialRepository extends JpaRepository<NGeniusCredential, Long> {
}
