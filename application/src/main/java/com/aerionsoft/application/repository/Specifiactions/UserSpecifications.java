package com.aerionsoft.application.repository.Specifiactions;

import com.aerionsoft.application.entity.client.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    public static Specification<User> hasAgency(boolean isAgency) {
        return (root, query, cb) -> cb.equal(root.get("isAgency"), isAgency);
    }

    public static Specification<User> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<User> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isEmpty()) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<User> createdOn(String creationDate) {
        return (root, query, cb) -> {
            if (creationDate == null || creationDate.isEmpty()) return null;
            return cb.equal(cb.function("DATE", java.sql.Date.class, root.get("createdAt")), java.sql.Date.valueOf(creationDate));
        };
    }
}