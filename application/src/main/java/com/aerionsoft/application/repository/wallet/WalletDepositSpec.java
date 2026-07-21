package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class WalletDepositSpec {

    private WalletDepositSpec() {}

    public static Specification<WalletDeposit> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    /** userId = parentId  OR  actingUserId = childId */
    public static Specification<WalletDeposit> forUserOrActing(Long userId, Long actingUserId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("userId"), userId),
                cb.equal(root.get("actingUserId"), actingUserId)
        );
    }

    public static Specification<WalletDeposit> hasStatus(DepositStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<WalletDeposit> hasStatuses(List<DepositStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<WalletDeposit> hasType(DepositType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<WalletDeposit> hasTypes(Collection<DepositType> types) {
        return (root, query, cb) -> root.get("type").in(types);
    }

    public static Specification<WalletDeposit> hasCurrency(Currency currency) {
        return (root, query, cb) -> cb.equal(root.get("currency"), currency);
    }

    public static Specification<WalletDeposit> createdAfter(LocalDate from) {
        Specification<WalletDeposit> spec = OffsetAwareDateSpec.createdAtInUserRange(
                from, null, "createdAt", "createdTimeOffset");
        if (spec == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return spec;
    }

    public static Specification<WalletDeposit> createdBefore(LocalDate to) {
        Specification<WalletDeposit> spec = OffsetAwareDateSpec.createdAtInUserRange(
                null, to, "createdAt", "createdTimeOffset");
        if (spec == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return spec;
    }
}

