package com.aerionsoft.application.repository.email;

import com.aerionsoft.application.entity.email.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByTemplateNameAndIsActiveTrueAndBusinessId(String templateName, Long businessId);

    List<EmailTemplate> findByIsActiveTrueAndBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<EmailTemplate> findByIdAndBusinessId(Long id, Long businessId);

    @Query("SELECT e FROM EmailTemplate e WHERE e.templateType = :templateType AND e.isActive = true AND e.businessId = :businessId")
    List<EmailTemplate> findByTemplateTypeAndBusinessId(String templateType, Long businessId);
}
