package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.entity.client.SupplierTransactionHistory;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class SupplierTransactionHistorySpecification {
    public static Specification<SupplierTransactionHistory> hasAgencyId(Long agencyId) {
        return (root, query, cb) ->
                agencyId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("agencyId"), agencyId);
    }

    public static Specification<SupplierTransactionHistory> hasSupplierId(Long supplierId) {
        return (root, query, cb) ->
                supplierId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("supplierId"), supplierId);
    }

    public static Specification<SupplierTransactionHistory> hasLedgerId(Long ledgerId) {
        return (root, query, cb) ->
                ledgerId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("ledgerId"), ledgerId);
    }

    public static Specification<SupplierTransactionHistory> createdInUserRange(LocalDate from, LocalDate to) {
        Specification<SupplierTransactionHistory> spec = OffsetAwareDateSpec.createdAtInUserRange(
                from, to, "createdDate", "createdTimeOffset");
        return spec != null ? spec : (root, query, cb) -> cb.conjunction();
    }
}
