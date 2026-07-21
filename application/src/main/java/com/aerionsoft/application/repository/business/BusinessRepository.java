package com.aerionsoft.application.repository.business;

import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.business.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BusinessRepository extends JpaRepository<BusinessEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<BusinessEntity> {

    Optional<BusinessEntity> findFirstByMotherUser(User user);

    Optional<BusinessEntity> findByMotherUserId(Long id);

    @Query("SELECT b FROM BusinessEntity b WHERE b.motherUser.id IN :userIds")
    List<BusinessEntity> findByMotherUserIdIn(@Param("userIds") Collection<Long> userIds);

    @Query("SELECT b.motherUser.id, b.companyName FROM BusinessEntity b WHERE b.motherUser.id IN :userIds")
    List<Object[]> findCompanyNamesByMotherUserIds(@Param("userIds") Collection<Long> userIds);

    boolean existsByMotherUser(User user);

    List<BusinessEntity> findByCreditLimitGreaterThan(java.math.BigDecimal value);

    // Count businesses by status
    Long countByStatus(BusinessStatus status);

    @Query("SELECT b FROM BusinessEntity b WHERE (:currency IS NULL OR b.motherUser.currency = :currency)")
    Page<BusinessEntity> findByMotherUserCurrency(@Param("currency") String currency, Pageable pageable);

    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // -----------------------------------------------------------------------
    // Agency Due (negative balance) queries
    // -----------------------------------------------------------------------

    @Query("SELECT b FROM BusinessEntity b WHERE b.motherUser.balance < 0 " +
           "AND b.createdAt >= :from " +
           "AND b.createdAt <= :to")
    Page<BusinessEntity> findAgenciesDue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT b FROM BusinessEntity b WHERE b.motherUser.balance < 0 " +
           "AND b.createdAt >= :from " +
           "AND b.createdAt <= :to " +
           "AND b.motherUser.currency = :currency")
    Page<BusinessEntity> findAgenciesDueByCurrency(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("currency") String currency,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.motherUser.balance), 0.0) FROM BusinessEntity b WHERE b.motherUser.balance < 0 " +
           "AND b.createdAt >= :from " +
           "AND b.createdAt <= :to")
    Double sumAgencyDue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(b.motherUser.balance), 0.0) FROM BusinessEntity b WHERE b.motherUser.balance < 0 " +
           "AND b.createdAt >= :from " +
           "AND b.createdAt <= :to " +
           "AND b.motherUser.currency = :currency")
    Double sumAgencyDueByCurrency(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("currency") String currency);
    // -----------------------------------------------------------------------
    // Agency Credit (positive balance) queries
    // -----------------------------------------------------------------------

    @Query("SELECT b FROM BusinessEntity b WHERE b.motherUser.balance > 0 " +
           "AND b.createdAt >= :from " +
           "AND b.createdAt <= :to")
    Page<BusinessEntity> findAgenciesCredit(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT b FROM BusinessEntity b WHERE b.motherUser.balance > 0 " +
           "AND b.createdAt >= :from " +
           "AND b.createdAt <= :to " +
           "AND b.motherUser.currency = :currency")
    Page<BusinessEntity> findAgenciesCreditByCurrency(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("currency") String currency,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.motherUser.balance), 0.0) FROM BusinessEntity b WHERE b.motherUser.balance > 0 " +
           "AND b.createdAt >= :from " +
           "AND b.createdAt <= :to")
    Double sumAgencyCredit(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(b.motherUser.balance), 0.0) FROM BusinessEntity b WHERE b.motherUser.balance > 0 " +
           "AND b.createdAt >= :from " +
           "AND b.createdAt <= :to " +
           "AND b.motherUser.currency = :currency")
    Double sumAgencyCreditByCurrency(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("currency") String currency);

    // -----------------------------------------------------------------------
    // No-filter variants (sort direction controlled via Pageable)
    // Native SQL used to bypass the @Where(is_deleted=false) filter on User.
    // -----------------------------------------------------------------------

    @Query(value = "SELECT b.*, u.balance AS mother_user_balance FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance < 0",
           countQuery = "SELECT COUNT(*) FROM businesses b " +
                        "JOIN users u ON b.mother_user_id = u.id " +
                        "WHERE u.balance < 0",
           nativeQuery = true)
    Page<BusinessEntity> findAgenciesDueNoFilter(Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(u.balance), 0.0) FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance < 0",
           nativeQuery = true)
    Double sumAgencyDueNoFilter();

    @Query(value = "SELECT b.*, u.balance AS mother_user_balance FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance > 0",
           countQuery = "SELECT COUNT(*) FROM businesses b " +
                        "JOIN users u ON b.mother_user_id = u.id " +
                        "WHERE u.balance > 0",
           nativeQuery = true)
    Page<BusinessEntity> findAgenciesCreditNoFilter(Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(u.balance), 0.0) FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance > 0",
           nativeQuery = true)
    Double sumAgencyCreditNoFilter();

    // -----------------------------------------------------------------------
    // Currency-filtered variants (currency = null means all currencies)
    // Native SQL used to bypass the @Where(is_deleted=false) filter on User.
    // -----------------------------------------------------------------------

    @Query(value = "SELECT b.*, u.balance AS mother_user_balance FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance < 0 " +
                   "AND (:currency IS NULL OR u.currency = :currency)",
           countQuery = "SELECT COUNT(*) FROM businesses b " +
                        "JOIN users u ON b.mother_user_id = u.id " +
                        "WHERE u.balance < 0 " +
                        "AND (:currency IS NULL OR u.currency = :currency)",
           nativeQuery = true)
    Page<BusinessEntity> findAgenciesDueWithCurrency(@Param("currency") String currency, Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(u.balance), 0.0) FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance < 0 " +
                   "AND (:currency IS NULL OR u.currency = :currency)",
           nativeQuery = true)
    Double sumAgencyDueWithCurrency(@Param("currency") String currency);

    @Query(value = "SELECT u.currency, SUM(u.balance) FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance < 0 " +
                   "AND (:currency IS NULL OR u.currency = :currency) " +
                   "GROUP BY u.currency",
           nativeQuery = true)
    List<Object[]> sumAgencyDueGroupedByCurrency(@Param("currency") String currency);

    @Query(value = "SELECT b.*, u.balance AS mother_user_balance FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance > 0 " +
                   "AND (:currency IS NULL OR u.currency = :currency)",
           countQuery = "SELECT COUNT(*) FROM businesses b " +
                        "JOIN users u ON b.mother_user_id = u.id " +
                        "WHERE u.balance > 0 " +
                        "AND (:currency IS NULL OR u.currency = :currency)",
           nativeQuery = true)
    Page<BusinessEntity> findAgenciesCreditWithCurrency(@Param("currency") String currency, Pageable pageable);

    @Query(value = "SELECT COALESCE(SUM(u.balance), 0.0) FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance > 0 " +
                   "AND (:currency IS NULL OR u.currency = :currency)",
           nativeQuery = true)
    Double sumAgencyCreditWithCurrency(@Param("currency") String currency);

    @Query(value = "SELECT u.currency, SUM(u.balance) FROM businesses b " +
                   "JOIN users u ON b.mother_user_id = u.id " +
                   "WHERE u.balance > 0 " +
                   "AND (:currency IS NULL OR u.currency = :currency) " +
                   "GROUP BY u.currency",
           nativeQuery = true)
    List<Object[]> sumAgencyCreditGroupedByCurrency(@Param("currency") String currency);

    @Query("SELECT b FROM BusinessEntity b WHERE " +
           "(:currency IS NULL OR b.motherUser.currency = :currency) AND " +
           "(:query IS NULL OR LOWER(b.companyName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<BusinessEntity> findByMotherUserCurrencyAndQuery(
            @Param("currency") String currency,
            @Param("query") String query,
            Pageable pageable);
}
