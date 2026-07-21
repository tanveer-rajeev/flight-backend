package com.aerionsoft.application.repository.flight;

import com.aerionsoft.application.entity.MarkupPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarkupPlanRepository extends JpaRepository<MarkupPlan, Long> {
}

