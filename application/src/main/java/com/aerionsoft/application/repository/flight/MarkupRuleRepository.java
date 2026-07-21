package com.aerionsoft.application.repository.flight;

import com.aerionsoft.application.entity.MarkupRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarkupRuleRepository extends JpaRepository<MarkupRule, Long> {
    List<MarkupRule> findByIsActiveTrueOrderByPriorityDesc();

    List<MarkupRule> findByMarkupPlan_IdOrderByPriorityDesc(Long markupPlanId);

    @Query("SELECT r FROM MarkupRule r WHERE r.isActive = true " +
           "AND (r.markupPlan.targetUserType = :userType OR r.markupPlan.targetUserType = 'ANY' OR r.markupPlan.targetUserType IS NULL) " +
           "ORDER BY r.priority DESC")
    List<MarkupRule> findByUserTypeOrderByPriorityDesc(@Param("userType") String userType);


    @Query("SELECT r FROM MarkupRule r WHERE r.isActive = true " +
           "AND (r.markupPlan.targetUserType = 'AGENT' OR r.markupPlan.targetUserType = 'ANY' OR r.markupPlan.targetUserType IS NULL) " +
           "AND EXISTS (SELECT mpb FROM MarkupPlanBusiness mpb WHERE mpb.markupPlan.id = r.markupPlan.id " +
           "AND mpb.businessId = :businessId AND mpb.isActive = true) " +
           "ORDER BY r.priority DESC")
    List<MarkupRule> findByBusinessIdOrderByPriorityDesc(@Param("businessId") Long businessId);
}
