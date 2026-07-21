package com.aerionsoft.application.repository.sms;

import com.aerionsoft.application.entity.SmsCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsCredentialsRepository extends JpaRepository<SmsCredentials, Long> {

    @Query("SELECT s FROM SmsCredentials s WHERE s.isActive = true ORDER BY s.id DESC")
    Optional<SmsCredentials> findActiveCredentials();

    Optional<SmsCredentials> findByIsActiveTrue();
}
