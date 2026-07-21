package com.aerionsoft.application.repository.expense;

import com.aerionsoft.application.entity.expense.ExpenseDetail;
import com.aerionsoft.application.enums.common.UsingPortal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseDetailRepository extends JpaRepository<ExpenseDetail, Long> {

    List<ExpenseDetail> findByExpenseId(Long expenseId);

    List<ExpenseDetail> findByAccountHeadId(Long accountHeadId);

    List<ExpenseDetail> findByUsingPortal(UsingPortal usingPortal);

    List<ExpenseDetail> findByUsingPortalAndPortalId(UsingPortal usingPortal, Long portalId);

    @Query("SELECT SUM(ed.itemAmount) FROM ExpenseDetail ed WHERE ed.expenseId = :expenseId")
    BigDecimal calculateTotalByExpenseId(Long expenseId);

    void deleteByExpenseId(Long expenseId);

    // Single record access with filtering
    Optional<ExpenseDetail> findByIdAndUsingPortal(Long id, UsingPortal usingPortal);

    Optional<ExpenseDetail> findByIdAndCreatedBy(Long id, Long createdBy);

    List<ExpenseDetail> findByCreatedBy(Long createdBy);

    List<ExpenseDetail> findByAccountHeadIdIn(List<Long> accountHeadIds);

    @Query("SELECT COALESCE(SUM(ed.itemAmount), 0) FROM ExpenseDetail ed WHERE ed.accountHead.id IN :accountHeadIds")
    BigDecimal getTotalItemAmountByAccountHeadIds(List<Long> accountHeadIds);
}
