package com.aerionsoft.application.repository.cms;

import com.aerionsoft.application.entity.cms.CustomForm;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomFormRepository extends JpaRepository<CustomForm, Long> {
    Optional<CustomForm> findBySlug(String slug);

    @EntityGraph(attributePaths = {"sections", "sections.fields"})
    Optional<CustomForm> findWithSectionsAndFieldsById(Long id);
}
