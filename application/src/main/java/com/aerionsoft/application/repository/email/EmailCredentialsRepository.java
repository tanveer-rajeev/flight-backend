package com.aerionsoft.application.repository.email;

import com.aerionsoft.application.entity.email.EmailCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailCredentialsRepository extends JpaRepository<EmailCredentials, Long> {

    @Query("SELECT e FROM EmailCredentials e WHERE e.isActive = true AND e.businessId = :businessId ORDER BY e.id DESC")
    Optional<EmailCredentials> findActiveCredentialsByBusinessId(@Param("businessId") Long businessId);
}
