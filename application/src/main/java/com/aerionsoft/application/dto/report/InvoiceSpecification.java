package com.aerionsoft.application.dto.report;

import com.aerionsoft.application.entity.client.Invoice;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class InvoiceSpecification {

    public static Specification<Invoice> filterBy(Long agencyId, LocalDate from, LocalDate to) {
        Specification<Invoice> spec = (root, query, cb) -> cb.conjunction();

        if (agencyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("agencyUser").get("id"), agencyId));
        }

        Specification<Invoice> dateSpec = OffsetAwareDateSpec.createdAtInUserRange(
                from, to, "createdAt", "createdTimeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }

        return spec;
    }
}
