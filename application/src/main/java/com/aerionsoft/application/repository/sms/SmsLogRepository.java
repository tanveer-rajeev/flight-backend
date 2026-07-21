package com.aerionsoft.application.repository.sms;

import com.aerionsoft.application.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    List<SmsLog> findByStatusOrderByCreatedAtDesc(SmsLog.SmsStatus status);

    @Query("SELECT s FROM SmsLog s WHERE s.sentAt BETWEEN :startDate AND :endDate ORDER BY s.sentAt DESC")
    List<SmsLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
