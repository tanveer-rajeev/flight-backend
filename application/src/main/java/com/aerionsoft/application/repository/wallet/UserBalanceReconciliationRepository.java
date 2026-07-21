package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.view.UserBalanceReconciliation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBalanceReconciliationRepository
        extends JpaRepository<UserBalanceReconciliation, Long> {

    /** Return only rows where diff_amount != 0 (discrepancies). */
    @Query("SELECT r FROM UserBalanceReconciliation r WHERE r.diffAmount <> 0")
    Page<UserBalanceReconciliation> findDiscrepancies(Pageable pageable);
}

