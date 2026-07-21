package com.aerionsoft.application.repository.email;

import com.aerionsoft.application.entity.email.EmailLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogsRepository extends JpaRepository<EmailLogs, Long> {

    List<EmailLogs> findByToEmailOrderByCreatedAtDesc(String toEmail);

    List<EmailLogs> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT e FROM EmailLogs e WHERE e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    List<EmailLogs> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(e) FROM EmailLogs e WHERE e.status = :status")
    long countByStatus(@Param("status") String status);
}
