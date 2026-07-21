package com.aerionsoft.application.repository.email;

import com.aerionsoft.application.entity.email.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByStatusOrderByCreatedAtDesc(EmailLog.EmailStatus status);

    @Query("SELECT e FROM EmailLog e WHERE e.sentAt BETWEEN :startDate AND :endDate ORDER BY e.sentAt DESC")
    List<EmailLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e FROM EmailLog e WHERE e.toEmail = :email ORDER BY e.createdAt DESC")
    List<EmailLog> findByToEmail(@Param("email") String email);
}
