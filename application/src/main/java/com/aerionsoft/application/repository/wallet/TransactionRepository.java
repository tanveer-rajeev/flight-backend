package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.wallet.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Transaction> {
    List<Transaction> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    long countByUserIdIn(Collection<Long> userIds);

    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.active = true ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Optional<Transaction> findFirstBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<Transaction> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.type IN :types AND t.active = true ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdAndTypeIn(@Param("userId") Long userId, @Param("types") List<String> types, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type IN :types AND t.active = true ORDER BY t.createdAt DESC")
    Page<Transaction> findByTypeIn(@Param("types") List<String> types, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.active = true ORDER BY t.createdAt DESC")
    List<Transaction> findAllByUserIdUnpaged(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.type IN :types AND t.active = true ORDER BY t.createdAt DESC")
    List<Transaction> findAllByUserIdAndTypeInUnpaged(@Param("userId") Long userId, @Param("types") List<String> types);

    @Query("SELECT t FROM Transaction t WHERE t.type IN :types AND t.active = true ORDER BY t.createdAt DESC")
    List<Transaction> findAllByTypeInUnpaged(@Param("types") List<String> types);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.active = true AND t.createdAt < :before")
    List<Transaction> findAllByUserIdAndCreatedAtBefore(@Param("userId") Long userId, @Param("before") LocalDateTime before);

    @Query("SELECT t FROM Transaction t WHERE t.active = true AND t.createdAt < :before")
    List<Transaction> findAllByCreatedAtBefore(@Param("before") LocalDateTime before);

    void deleteByUserId(Long userId);
}
