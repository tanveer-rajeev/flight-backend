package com.aerionsoft.application.repository.flight;

import com.aerionsoft.application.entity.MarkupPlanBusiness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkupPlanBusinessRepository extends JpaRepository<MarkupPlanBusiness, Long> {

    List<MarkupPlanBusiness> findByMarkupPlanIdAndIsActiveTrue(Long markupPlanId);

    List<MarkupPlanBusiness> findByBusinessIdAndIsActiveTrue(Long businessId);

    Optional<MarkupPlanBusiness> findByMarkupPlanIdAndBusinessId(Long markupPlanId, Long businessId);

    @Query("SELECT mpb FROM MarkupPlanBusiness mpb WHERE mpb.markupPlan.id = :markupPlanId")
    List<MarkupPlanBusiness> findAllByMarkupPlanId(@Param("markupPlanId") Long markupPlanId);

    Page<MarkupPlanBusiness> findAllByIsActiveFalse(Pageable pageable);

    void deleteByMarkupPlanIdAndBusinessId(Long markupPlanId, Long businessId);

    List<MarkupPlanBusiness> findAllByIsActiveTrue();

    // Paginated queries
    Page<MarkupPlanBusiness> findAllByIsActiveTrue(Pageable pageable);

    @NonNull
    Page<MarkupPlanBusiness> findAll(@NonNull Pageable pageable);
}



