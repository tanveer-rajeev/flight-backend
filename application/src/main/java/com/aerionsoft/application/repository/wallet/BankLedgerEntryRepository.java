package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.wallet.BankLedgerEntry;
import com.aerionsoft.application.enums.wallet.BankLedgerSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BankLedgerEntryRepository extends JpaRepository<BankLedgerEntry, Long>, JpaSpecificationExecutor<BankLedgerEntry> {

    boolean existsBySourceTypeAndSourceId(BankLedgerSourceType sourceType, Long sourceId);

    Optional<BankLedgerEntry> findBySourceTypeAndSourceId(BankLedgerSourceType sourceType, Long sourceId);

    Page<BankLedgerEntry> findByBankIdOrderByCreatedAtDesc(Long bankId, Pageable pageable);

    List<BankLedgerEntry> findByBankIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            Long bankId, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT e FROM BankLedgerEntry e
            WHERE e.bank.id = :bankId
              AND e.createdAt >= :from
              AND e.createdAt < :to
            ORDER BY e.createdAt ASC
            """)
    List<BankLedgerEntry> findStatementEntries(
            @Param("bankId") Long bankId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT e FROM BankLedgerEntry e
            WHERE e.bank.id = :bankId
              AND e.createdAt < :before
            ORDER BY e.createdAt ASC
            """)
    List<BankLedgerEntry> findEntriesBefore(@Param("bankId") Long bankId, @Param("before") LocalDateTime before);

    @Query("""
            SELECT e FROM BankLedgerEntry e
            WHERE e.bank.id = :bankId
              AND e.createdAt >= :from
              AND e.createdAt < :to
            ORDER BY e.createdAt DESC
            """)
    Page<BankLedgerEntry> findFilteredEntries(
            @Param("bankId") Long bankId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
