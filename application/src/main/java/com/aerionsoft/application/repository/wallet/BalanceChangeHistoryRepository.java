package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.wallet.BalanceChangeHistory;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BalanceChangeHistoryRepository extends JpaRepository<BalanceChangeHistory, Long>, JpaSpecificationExecutor<BalanceChangeHistory> {

    static Specification<BalanceChangeHistory> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    static Specification<BalanceChangeHistory> hasChangeType(String changeType) {
        return (root, query, cb) ->
                changeType == null || changeType.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("changeType"), changeType);
    }

    static Specification<BalanceChangeHistory> createdInUserRange(LocalDate from, LocalDate to) {
        Specification<BalanceChangeHistory> spec = OffsetAwareDateSpec.createdAtInUserRange(
                from, to, "createdAt", "createdTimeOffset");
        return spec != null ? spec : (root, query, cb) -> cb.conjunction();
    }

    void deleteByUserId(Long userId);

    List<BalanceChangeHistory> findByUserIdAndReferenceTypeAndReferenceIdAndChangeType(
            Long userId, String referenceType, Long referenceId, String changeType);

    java.util.Optional<BalanceChangeHistory> findFirstByUserIdAndReferenceTypeAndReferenceIdIsNullAndChangeTypeAndAmount(
            Long userId, String referenceType, String changeType, Double amount);
}
