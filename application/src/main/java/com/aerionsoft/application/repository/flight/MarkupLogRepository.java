package com.aerionsoft.application.repository.flight;

import com.aerionsoft.application.entity.MarkupLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarkupLogRepository extends JpaRepository<MarkupLog, Long> {
}

