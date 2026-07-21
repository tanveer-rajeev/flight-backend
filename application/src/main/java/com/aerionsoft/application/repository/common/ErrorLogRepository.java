package com.aerionsoft.application.repository.common;

import com.aerionsoft.application.entity.ErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long>, JpaSpecificationExecutor<ErrorLog> {

    Page<ErrorLog> findByServiceNameOrderByCreatedAtDesc(String serviceName, Pageable pageable);

    Page<ErrorLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ErrorLog> findByTraceIdOrderByCreatedAtDesc(String traceId, Pageable pageable);

    @Query("""
            SELECT e FROM ErrorLog e
            WHERE (:traceId IS NULL OR e.traceId = :traceId)
              AND (:userId IS NULL OR e.userId = :userId)
              AND (:serviceName IS NULL OR e.serviceName = :serviceName)
              AND (:errorCode IS NULL OR e.errorCode = :errorCode)
              AND (:fromDate IS NULL OR e.createdAt >= :fromDate)
              AND (:toDate IS NULL OR e.createdAt <= :toDate)
            ORDER BY e.createdAt DESC
            """)
    Page<ErrorLog> search(
            @Param("traceId") String traceId,
            @Param("userId") Long userId,
            @Param("serviceName") String serviceName,
            @Param("errorCode") String errorCode,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT e FROM ErrorLog e WHERE e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    Page<ErrorLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    @Query("SELECT e FROM ErrorLog e WHERE e.createdAt < :cutoffDate ORDER BY e.createdAt DESC")
    List<ErrorLog> findByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT e.serviceName, COUNT(e) FROM ErrorLog e GROUP BY e.serviceName")
    List<Object[]> countErrorsByService();

    @Modifying
    @Transactional
    @Query("DELETE FROM ErrorLog e WHERE e.createdAt < :cutoffDate")
    int deleteOldErrorLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM error_log
            WHERE ctid IN (
                SELECT ctid FROM error_log
                WHERE created_at < :cutoffDate
                LIMIT :batchSize
            )
            """, nativeQuery = true)
    int deleteOldErrorLogsBatch(@Param("cutoffDate") LocalDateTime cutoffDate,
                                @Param("batchSize") int batchSize);

    @Query("SELECT e.serviceName, COUNT(e) FROM ErrorLog e WHERE e.createdAt >= :since GROUP BY e.serviceName")
    List<Object[]> countRecentErrorsByService(@Param("since") LocalDateTime since);

    Page<ErrorLog> findAllByOrderByIdDesc(Pageable pageable);
}
