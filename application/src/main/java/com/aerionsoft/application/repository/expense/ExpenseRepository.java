package com.aerionsoft.application.repository.expense;

import com.aerionsoft.application.entity.expense.Expense;
import com.aerionsoft.application.enums.expense.ExpenseStatus;
import com.aerionsoft.application.enums.common.UsingPortal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    List<Expense> findByExpenseStatus(ExpenseStatus status);

    List<Expense> findByCreatedBy(Long createdBy);

    List<Expense> findByUsingPortal(UsingPortal usingPortal);

    List<Expense> findByUsingPortalAndPortalId(UsingPortal usingPortal, Long portalId);

    List<Expense> findByExpenseStatusAndUsingPortal(ExpenseStatus status, UsingPortal usingPortal);

    // Pagination methods
    Page<Expense> findByUsingPortal(UsingPortal usingPortal, Pageable pageable);

    Page<Expense> findByCreatedBy(Long createdBy, Pageable pageable);

    // Single record access with filtering
    Optional<Expense> findByIdAndUsingPortal(Long id, UsingPortal usingPortal);

    Optional<Expense> findByIdAndCreatedBy(Long id, Long createdBy);

    @Query("SELECT COALESCE(SUM(i.expenseAmount), 0) FROM Expense i")
    BigDecimal getTotalExpenseAmount();
}
