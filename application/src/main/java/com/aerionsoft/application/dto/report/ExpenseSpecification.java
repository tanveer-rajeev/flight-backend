package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.entity.expense.Expense;
import com.aerionsoft.application.entity.expense.ExpenseDetail;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ExpenseSpecification {
    public static Specification<Expense> filterBy(
            Long portalId,
            LocalDate from,
            LocalDate to
    ) {
        return (root, query, cb) -> {
            Join<Expense, ExpenseDetail> details = root.join("expenseDetails", JoinType.LEFT);

            query.groupBy(root.get("id"), root.get("expenseTitle"), root.get("createdAt"));
            query.multiselect(
                    root.get("id"),
                    root.get("expenseTitle"),
                    root.get("createdAt"),
                    cb.coalesce(cb.sum(details.get("itemAmount")), 0)
            );

            Predicate predicate = cb.conjunction();

            if (portalId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("portalId"), portalId));
            }

            Specification<Expense> dateSpec = OffsetAwareDateSpec.createdAtInUserRange(
                    from, to, "createdAt", "createdTimeOffset");
            if (dateSpec != null) {
                Predicate datePredicate = dateSpec.toPredicate(root, query, cb);
                if (datePredicate != null) {
                    predicate = cb.and(predicate, datePredicate);
                }
            }

            return predicate;
        };
    }
}
