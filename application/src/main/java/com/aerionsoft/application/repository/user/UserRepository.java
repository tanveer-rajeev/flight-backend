package com.aerionsoft.application.repository.user;

import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    List<User> findAllByBusiness(BusinessEntity business);

    List<User> findByIsAgency(boolean isAgency, Pageable pageable);

    Page<User> findAll(Pageable pageable);

    Long countByIsAgency(boolean isAgency);

    List<User> findByIsAgency(boolean isAgency);


    Page<User> findAll(Specification<User> spec, Pageable pageable);

    List<User> findByEmailIn(List<String> emails);

    List<User> findByIsAgencyAndCreatedAtBetween(boolean isAgency, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    User findByIdAndIsAgency(Long id, boolean isAgency);

    List<User> findByParentUser_Id(Long parentUserId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.parentUser WHERE u.id IN :ids")
    List<User> findByIdInWithParent(@Param("ids") Collection<Long> ids);

    Boolean existsByEmail(String email);

    List<User> findByBusiness(BusinessEntity business);

    List<User> findByBusinessId(Long businessId);

    Optional<User> findByCode(String code);

    List<User> findByCodeContainingIgnoreCase(String code);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.code) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchByNameEmailPhoneOrCode(@Param("query") String query);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN u.business b
            WHERE (:businessId IS NULL OR b.id = :businessId)
              AND (
                   :query IS NULL OR :query = ''
                   OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(u.phoneNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(u.code, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(b.companyName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY u.fullName ASC, u.email ASC
            """)
    Page<User> searchForLiveChat(@Param("query") String query,
                                 @Param("businessId") Long businessId,
                                 Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.balance = :balance WHERE u.id = :userId")
    int updateBalance(@Param("userId") Long userId, @Param("balance") Double balance);

    @Modifying
    @Query("DELETE FROM User u WHERE u.isVerified = false AND u.createdAt < :cutoffTime")
    int deleteByIsVerifiedFalseAndCreatedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT u FROM User u WHERE :role MEMBER OF u.roles")
    List<User> findByRole(@Param("role") String role);

    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
