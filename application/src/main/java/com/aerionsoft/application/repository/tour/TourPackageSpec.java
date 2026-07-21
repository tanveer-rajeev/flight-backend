package com.aerionsoft.application.repository.tour;

import com.aerionsoft.application.entity.tour.TourPackage;
import org.springframework.data.jpa.domain.Specification;

public final class TourPackageSpec {

    private TourPackageSpec() {
    }

    public static Specification<TourPackage> matchesKeyword(String keyword) {
        String pattern = "%" + escapeLikePattern(keyword).toLowerCase() + "%";
        char escape = '\\';
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, escape),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern, escape),
                cb.like(cb.lower(root.get("destinationCity")), pattern, escape),
                cb.like(cb.lower(root.get("destinationCountry")), pattern, escape)
        );
    }

    public static Specification<TourPackage> hasStatus(TourPackage.PackageStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static String escapeLikePattern(String keyword) {
        return keyword.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
