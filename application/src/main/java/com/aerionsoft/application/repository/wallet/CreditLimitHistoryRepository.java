package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.dto.report.CreditListProjection;
import com.aerionsoft.application.entity.CreditLimitHistory;
import com.aerionsoft.application.enums.wallet.CreditLimitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CreditLimitHistoryRepository extends JpaRepository<CreditLimitHistory, Long> {

    Page<CreditLimitHistory> findByBusinessId(Long businessId, Pageable pageable);

    List<CreditLimitHistory> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Page<CreditLimitHistory> findByBusinessIdAndStatus(Long businessId, CreditLimitStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN c.status = 'CREDIT' THEN c.amount ELSE 0 END), 0) - " +
           "COALESCE(SUM(CASE WHEN c.status = 'DEBIT' THEN c.amount ELSE 0 END), 0) " +
           "FROM CreditLimitHistory c WHERE c.businessId = :businessId")
    BigDecimal calculateNetCreditByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT c FROM CreditLimitHistory c WHERE c.businessId = :businessId AND c.createdAt BETWEEN :startDate AND :endDate")
    List<CreditLimitHistory> findByBusinessIdAndDateRange(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM CreditLimitHistory c WHERE c.createdBy = :createdBy ORDER BY c.createdAt DESC")
    Page<CreditLimitHistory> findByCreatedBy(@Param("createdBy") Long createdBy, Pageable pageable);

    Page<CreditLimitHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CreditLimitHistory> findByBusinessIdAndCreatedAtBefore(Long businessId, LocalDateTime before);

    List<CreditLimitHistory> findByCreatedAtBefore(LocalDateTime before);

    void deleteByBusinessId(Long businessId);

    @Query(value = "SELECT c.* FROM credit_limit_history c " +
                   "JOIN businesses b ON c.business_id = b.id " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE c.status = :status " +
                   "AND (CAST(:businessId AS BIGINT) IS NULL OR c.business_id = CAST(:businessId AS BIGINT)) " +
                   "AND (CAST(:currency AS VARCHAR) IS NULL OR u.currency = CAST(:currency AS VARCHAR)) " +
                   "AND (CAST(:startInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(c.created_time_offset, 'Asia/Dhaka'), c.created_at) >= CAST(:startInstant AS TIMESTAMP)) " +
                   "AND (CAST(:endInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(c.created_time_offset, 'Asia/Dhaka'), c.created_at) < CAST(:endInstant AS TIMESTAMP)) " +
                   "ORDER BY c.created_at DESC",
           countQuery = "SELECT COUNT(c.id) FROM credit_limit_history c " +
                        "JOIN businesses b ON c.business_id = b.id " +
                        "JOIN users u ON b.mother_user_id = u.id " +
                        "WHERE c.status = :status " +
                        "AND (CAST(:businessId AS BIGINT) IS NULL OR c.business_id = CAST(:businessId AS BIGINT)) " +
                        "AND (CAST(:currency AS VARCHAR) IS NULL OR u.currency = CAST(:currency AS VARCHAR)) " +
                        "AND (CAST(:startInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(c.created_time_offset, 'Asia/Dhaka'), c.created_at) >= CAST(:startInstant AS TIMESTAMP)) " +
                        "AND (CAST(:endInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(c.created_time_offset, 'Asia/Dhaka'), c.created_at) < CAST(:endInstant AS TIMESTAMP))",
           nativeQuery = true)
    Page<CreditLimitHistory> findReportByStatus(
            @Param("status") String status,
            @Param("businessId") Long businessId,
            @Param("currency") String currency,
            @Param("startInstant") Timestamp startInstant,
            @Param("endInstant") Timestamp endInstant,
            Pageable pageable);

    // -----------------------------------------------------------------------
    // Credit List report — per-business aggregated CREDIT vs DEBIT totals
    // -----------------------------------------------------------------------

    /**
     * Returns one row per business with aggregated credit_given (CREDIT+REFUND)
     * and credit_used (DEBIT) amounts, filtered by agencyId, currency and date range.
     * Only businesses that have at least one credit history entry are returned.
     * Date filter is placed on the JOIN so that businesses with no entries in range
     * still appear with zero totals (avoids accidental inner-join behaviour).
     */
    @Query(value = "SELECT b.id AS businessId, b.company_name AS companyName, u.currency AS currency, u.balance AS balance, b.credit_limit AS creditLimit " +
                   "FROM businesses b JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE b.credit_limit > 0 " +
                   "AND (CAST(:currency AS VARCHAR) IS NULL OR u.currency = CAST(:currency AS VARCHAR)) " +
                   "AND (CAST(:agencyId AS BIGINT) IS NULL OR b.id = CAST(:agencyId AS BIGINT)) " +
                   "ORDER BY b.credit_limit DESC",
           countQuery = "SELECT COUNT(*) FROM businesses b JOIN users u ON b.mother_user_id = u.id " +
                        "WHERE b.credit_limit > 0 " +
                        "AND (CAST(:currency AS VARCHAR) IS NULL OR u.currency = CAST(:currency AS VARCHAR)) " +
                        "AND (CAST(:agencyId AS BIGINT) IS NULL OR b.id = CAST(:agencyId AS BIGINT))",
           nativeQuery = true)
    Page<CreditListProjection> findCreditListDesc(
            @Param("currency") String currency,
            @Param("agencyId") Long agencyId,
            Pageable pageable);

    @Query(value = "SELECT b.id AS businessId, b.company_name AS companyName, u.currency AS currency, u.balance AS balance, b.credit_limit AS creditLimit " +
                   "FROM businesses b JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE b.credit_limit > 0 " +
                   "AND (CAST(:currency AS VARCHAR) IS NULL OR u.currency = CAST(:currency AS VARCHAR)) " +
                   "AND (CAST(:agencyId AS BIGINT) IS NULL OR b.id = CAST(:agencyId AS BIGINT)) " +
                   "ORDER BY b.credit_limit ASC",
           countQuery = "SELECT COUNT(*) FROM businesses b JOIN users u ON b.mother_user_id = u.id " +
                        "WHERE b.credit_limit > 0 " +
                        "AND (CAST(:currency AS VARCHAR) IS NULL OR u.currency = CAST(:currency AS VARCHAR)) " +
                        "AND (CAST(:agencyId AS BIGINT) IS NULL OR b.id = CAST(:agencyId AS BIGINT))",
           nativeQuery = true)
    Page<CreditListProjection> findCreditListAsc(
            @Param("currency") String currency,
            @Param("agencyId") Long agencyId,
            Pageable pageable);


    @Query(value = "SELECT COALESCE(SUM(c.amount), 0) FROM credit_limit_history c " +
                   "JOIN businesses b ON c.business_id = b.id " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE c.status = :status " +
                   "AND (CAST(:businessId AS BIGINT) IS NULL OR c.business_id = CAST(:businessId AS BIGINT)) " +
                   "AND (CAST(:currency AS VARCHAR) IS NULL OR u.currency = CAST(:currency AS VARCHAR)) " +
                   "AND (CAST(:startInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(c.created_time_offset, 'Asia/Dhaka'), c.created_at) >= CAST(:startInstant AS TIMESTAMP)) " +
                   "AND (CAST(:endInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(c.created_time_offset, 'Asia/Dhaka'), c.created_at) < CAST(:endInstant AS TIMESTAMP))",
           nativeQuery = true)
    BigDecimal sumReportAmountByStatus(
            @Param("status") String status,
            @Param("businessId") Long businessId,
            @Param("currency") String currency,
            @Param("startInstant") Timestamp startInstant,
            @Param("endInstant") Timestamp endInstant);
}

