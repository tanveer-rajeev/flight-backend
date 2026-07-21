package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.dto.DepositSummaryDTO;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WalletDepositRepository extends JpaRepository<WalletDeposit, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<WalletDeposit> {
    List<WalletDeposit> findByUserId(Long userId);

    List<WalletDeposit> findByStatus(DepositStatus status);

    List<WalletDeposit> findByUserIdAndStatus(Long userId, DepositStatus status);

    List<WalletDeposit> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(Long userId, LocalDateTime from, LocalDateTime to);

    Page<WalletDeposit> findByUserId(Long userId, Pageable pageable);

    // For child users to access parent's deposits
    Page<WalletDeposit> findByUserIdOrActingUserId(Long userId, Long actingUserId, Pageable pageable);

    Page<WalletDeposit> findByStatus(DepositStatus status, Pageable pageable);

    Page<WalletDeposit> findByStatusAndCurrency(DepositStatus status, Currency currency, Pageable pageable);

    List<WalletDeposit> findByTypeIn(Collection<DepositType> type);

    List<WalletDeposit> findByTypeInAndCreatedAtBetween(Collection<DepositType> type, LocalDateTime createdAt, LocalDateTime createdAt2);

    /**
     * Deterministic single-row lookup even if historical duplicates exist.
     */
    Optional<WalletDeposit> findFirstByReferenceOrderByIdDesc(String reference);

    List<WalletDeposit> findByReferenceContainingIgnoreCase(String reference);

    /**
     * Count-based exists check to avoid any issues with duplicate rows.
     */
    @Query("SELECT (COUNT(d) > 0) FROM WalletDeposit d WHERE d.reference = :reference")
    boolean existsReference(@Param("reference") String reference);

    Boolean existsByUserIdAndAmountAndStatusAndCreatedAtAfter(Long userId, Double amount, DepositStatus status, LocalDateTime createdAt);

    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    Boolean existsByReferenceAndUserId(String reference, Long userId);

    // Count deposits by status
    Long countByStatus(DepositStatus status);

    Page<WalletDeposit> findByStatusIn(List<DepositStatus> statuses, Pageable pageable);

    Page<WalletDeposit> findByStatusInAndTypeIn(
            List<DepositStatus> statuses,
            Collection<DepositType> types,
            Pageable pageable
    );

    Page<WalletDeposit> findByStatusInAndTypeInAndCurrency(
            List<DepositStatus> statuses,
            Collection<DepositType> types,
            Currency currency,
            Pageable pageable
    );

    // Filter methods for deposits with status and type
    Page<WalletDeposit> findByUserIdAndStatus(Long userId, DepositStatus status, Pageable pageable);

    Page<WalletDeposit> findByUserIdAndType(Long userId, DepositType type, Pageable pageable);

    Page<WalletDeposit> findByUserIdAndStatusAndType(Long userId, DepositStatus status, DepositType type, Pageable pageable);

    Page<WalletDeposit> findByUserIdOrActingUserIdAndStatus(Long userId, Long actingUserId, DepositStatus status, Pageable pageable);

    Page<WalletDeposit> findByUserIdOrActingUserIdAndType(Long userId, Long actingUserId, DepositType type, Pageable pageable);

    Page<WalletDeposit> findByUserIdOrActingUserIdAndStatusAndType(Long userId, Long actingUserId, DepositStatus status, DepositType type, Pageable pageable);

    Page<WalletDeposit> findByType(DepositType type, Pageable pageable);


    Page<WalletDeposit> findByTypeIn(Collection<DepositType> type, Pageable pageable);

    Page<WalletDeposit> findByStatusAndType(DepositStatus status, DepositType type, Pageable pageable);

    Page<WalletDeposit> findByStatusAndTypeAndCurrency(
            DepositStatus status,
            DepositType type,
            Currency currency,
            Pageable pageable
    );

    Page<WalletDeposit> findByStatusInAndCurrency(
            List<DepositStatus> statuses,
            Currency currency,
            Pageable pageable
    );

    // User
    Page<WalletDeposit> findByUserIdAndCurrency(Long userId, Currency currency, Pageable pageable);

    Page<WalletDeposit> findByUserIdAndStatusAndCurrency(Long userId, DepositStatus status, Currency currency, Pageable pageable);

    Page<WalletDeposit> findByUserIdAndTypeAndCurrency(Long userId, DepositType type, Currency currency, Pageable pageable);

    Page<WalletDeposit> findByUserIdAndStatusAndTypeAndCurrency(
            Long userId,
            DepositStatus status,
            DepositType type,
            Currency currency,
            Pageable pageable
    );

    Page<WalletDeposit> findByUserIdOrActingUserIdAndStatusAndTypeAndCurrency(
            Long userId, Long actingUserId, DepositStatus status, DepositType type, Currency currency, Pageable pageable
    );

    Page<WalletDeposit> findByUserIdOrActingUserIdAndStatusAndCurrency(
            Long userId, Long actingUserId, DepositStatus status, Currency currency, Pageable pageable
    );

    Page<WalletDeposit> findByUserIdOrActingUserIdAndTypeAndCurrency(
            Long userId, Long actingUserId, DepositType type, Currency currency, Pageable pageable
    );

    Page<WalletDeposit> findByUserIdOrActingUserIdAndCurrency(
            Long userId, Long actingUserId, Currency currency, Pageable pageable
    );

    /**
     * Backward-compatible alias for older code paths.
     *
     * <p>IMPORTANT: In environments where duplicate references exist, callers should migrate to
     * {@link #findFirstByReferenceOrderByIdDesc(String)} to ensure deterministic results.
     */
    default Optional<WalletDeposit> findByReference(String reference) {
        return findFirstByReferenceOrderByIdDesc(reference);
    }

    // ── Deposit Report queries ──────────────────────────────────────────────────

    /**
     * Paginated deposit list restricted to reportable types:
     * CASH, CHEQUE, BANK_DEPOSIT, DEPOSIT, BANK_TRANSFER_OR_MFS, STRIPE, INSTANT, PURCHASE, NGENIUS, SSL.
     * All parameters are optional (pass null to skip that filter).
     */
    @Query(value = "SELECT d.* FROM wallet_deposit d " +
                   "WHERE d.status = 'APPROVED' " +
                   "AND d.type IN ('CASH','CHEQUE','BANK_DEPOSIT','DEPOSIT','BANK_TRANSFER_OR_MFS'," +
                   "               'STRIPE','INSTANT','PURCHASE','NGENIUS','SSL') " +
                   "AND (CAST(:type AS VARCHAR) IS NULL OR d.type = CAST(:type AS VARCHAR)) " +
                   "AND (:currency IS NULL OR d.currency = :currency) " +
                   "AND (:agencyId IS NULL OR d.user_id = :agencyId) " +
                   "AND (CAST(:startInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(d.created_time_offset, 'Asia/Dhaka'), d.created_at) >= CAST(:startInstant AS TIMESTAMP)) " +
                   "AND (CAST(:endInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(d.created_time_offset, 'Asia/Dhaka'), d.created_at) < CAST(:endInstant AS TIMESTAMP)) " +
                   "ORDER BY d.created_at DESC",
           countQuery = "SELECT COUNT(d.id) FROM wallet_deposit d " +
                        "WHERE d.status = 'APPROVED' " +
                        "AND d.type IN ('CASH','CHEQUE','BANK_DEPOSIT','DEPOSIT','BANK_TRANSFER_OR_MFS'," +
                        "               'STRIPE','INSTANT','PURCHASE','NGENIUS','SSL') " +
                        "AND (CAST(:type AS VARCHAR) IS NULL OR d.type = CAST(:type AS VARCHAR)) " +
                        "AND (:currency IS NULL OR d.currency = :currency) " +
                        "AND (:agencyId IS NULL OR d.user_id = :agencyId) " +
                        "AND (CAST(:startInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(d.created_time_offset, 'Asia/Dhaka'), d.created_at) >= CAST(:startInstant AS TIMESTAMP)) " +
                        "AND (CAST(:endInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(d.created_time_offset, 'Asia/Dhaka'), d.created_at) < CAST(:endInstant AS TIMESTAMP))",
           nativeQuery = true)
    Page<WalletDeposit> findDepositReport(
            @Param("type")      String type,
            @Param("currency")  Integer currency,
            @Param("agencyId")  Long agencyId,
            @Param("startInstant") Timestamp startInstant,
            @Param("endInstant")   Timestamp endInstant,
            Pageable pageable);

    /**
     * Sum of amounts matching the same filters as {@link #findDepositReport}.
     */
    @Query(value = "SELECT COALESCE(SUM(d.amount), 0) FROM wallet_deposit d " +
                   "WHERE d.status = 'APPROVED' " +
                   "AND d.type IN ('CASH','CHEQUE','BANK_DEPOSIT','DEPOSIT','BANK_TRANSFER_OR_MFS'," +
                   "               'STRIPE','INSTANT','PURCHASE','NGENIUS','SSL') " +
                   "AND (CAST(:type AS VARCHAR) IS NULL OR d.type = CAST(:type AS VARCHAR)) " +
                   "AND (:currency IS NULL OR d.currency = :currency) " +
                   "AND (:agencyId IS NULL OR d.user_id = :agencyId) " +
                   "AND (CAST(:startInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(d.created_time_offset, 'Asia/Dhaka'), d.created_at) >= CAST(:startInstant AS TIMESTAMP)) " +
                   "AND (CAST(:endInstant AS TIMESTAMP) IS NULL OR timezone(COALESCE(d.created_time_offset, 'Asia/Dhaka'), d.created_at) < CAST(:endInstant AS TIMESTAMP))",
           nativeQuery = true)
    BigDecimal sumDepositReport(
            @Param("type")      String type,
            @Param("currency")  Integer currency,
            @Param("agencyId")  Long agencyId,
            @Param("startInstant") Timestamp startInstant,
            @Param("endInstant")   Timestamp endInstant);

    @Query("""
    SELECT new com.aerionsoft.application.dto.DepositSummaryDTO(
        SUM(CASE WHEN d.status = 'PENDING' AND d.currency = com.aerionsoft.application.enums.common.Currency.BDT THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'PENDING' AND d.currency = com.aerionsoft.application.enums.common.Currency.INR THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'PENDING' AND d.currency = com.aerionsoft.application.enums.common.Currency.USD THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'PENDING' AND d.currency = com.aerionsoft.application.enums.common.Currency.PKR THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'PENDING' AND d.currency = com.aerionsoft.application.enums.common.Currency.SAR THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'PENDING' AND d.currency = com.aerionsoft.application.enums.common.Currency.QAR THEN d.amount ELSE 0 END),
            
                    SUM(CASE WHEN d.status = 'APPROVED' AND d.currency = com.aerionsoft.application.enums.common.Currency.BDT THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'APPROVED' AND d.currency = com.aerionsoft.application.enums.common.Currency.INR THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'APPROVED' AND d.currency = com.aerionsoft.application.enums.common.Currency.USD THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'APPROVED' AND d.currency = com.aerionsoft.application.enums.common.Currency.PKR THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'APPROVED' AND d.currency = com.aerionsoft.application.enums.common.Currency.SAR THEN d.amount ELSE 0 END),
                    SUM(CASE WHEN d.status = 'APPROVED' AND d.currency = com.aerionsoft.application.enums.common.Currency.QAR THEN d.amount ELSE 0 END)
    )
    FROM WalletDeposit d
    WHERE d.createdAt BETWEEN :start AND :end
""")
    DepositSummaryDTO getDepositSummaryByCurrency(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    void deleteByUserId(Long userId);
}

