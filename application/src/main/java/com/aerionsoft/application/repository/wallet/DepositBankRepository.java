package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.wallet.DepositBank;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepositBankRepository extends JpaRepository<DepositBank, Long> {
    List<DepositBank> findByIsActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DepositBank b WHERE b.id = :id")
    Optional<DepositBank> findByIdForUpdate(@Param("id") Long id);
}