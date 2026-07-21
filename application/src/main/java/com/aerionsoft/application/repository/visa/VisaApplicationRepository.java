package com.aerionsoft.application.repository.visa;

import com.aerionsoft.application.entity.visa.VisaApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisaApplicationRepository extends JpaRepository<VisaApplication, Long>, JpaSpecificationExecutor<VisaApplication> {
    List<VisaApplication> findByCreatedBy(String createdBy);

    long countByCreatedByAndSubmittedAtBetween(String createdBy, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
