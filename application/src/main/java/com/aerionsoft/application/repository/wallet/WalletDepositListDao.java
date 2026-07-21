package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.util.UserTimezoneUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional(readOnly = true)
public class WalletDepositListDao {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<WalletDeposit> findDeposits(DepositListFilter filter, Pageable pageable, boolean includeTotal) {
        QueryParts parts = buildQueryParts(filter);

        if (includeTotal) {
            List<WalletDeposit> content = fetchPage(parts, pageable, pageable.getPageSize());
            long total = count(parts);
            return new PageImpl<>(content, pageable, total);
        }

        int fetchSize = pageable.getPageSize() + 1;
        List<WalletDeposit> fetched = fetchPage(parts, pageable, fetchSize);
        boolean hasNext = fetched.size() > pageable.getPageSize();
        List<WalletDeposit> content = hasNext
                ? fetched.subList(0, pageable.getPageSize())
                : fetched;
        long total = pageable.getOffset() + content.size() + (hasNext ? 1 : 0);
        return new PageImpl<>(content, pageable, total);
    }

    private List<WalletDeposit> fetchPage(QueryParts parts, Pageable pageable, int limit) {
        TypedQuery<WalletDeposit> query = entityManager.createQuery(parts.dataJpql(), WalletDeposit.class);
        bindParams(query, parts.params());
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(limit);
        return query.getResultList();
    }

    private long count(QueryParts parts) {
        TypedQuery<Long> query = entityManager.createQuery(parts.countJpql(), Long.class);
        bindParams(query, parts.params());
        return query.getSingleResult();
    }

    private void bindParams(TypedQuery<?> query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private QueryParts buildQueryParts(DepositListFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (filter.admin()) {
            appendUserScope(where, params, filter);
        } else if (filter.hasActingScope()) {
            where.append(" AND (d.userId = :userId OR d.actingUserId = :actingUserId) ");
            params.put("userId", filter.userId());
            params.put("actingUserId", filter.actingUserId());
        } else if (filter.hasUserScope()) {
            where.append(" AND d.userId = :userId ");
            params.put("userId", filter.userId());
        }

        if (filter.statuses() != null && !filter.statuses().isEmpty()) {
            where.append(" AND d.status IN :statuses ");
            params.put("statuses", filter.statuses());
        }

        if (filter.types() != null && !filter.types().isEmpty()) {
            where.append(" AND d.type IN :types ");
            params.put("types", filter.types());
        }

        if (filter.currency() != null) {
            where.append(" AND d.currency = :currency ");
            params.put("currency", filter.currency());
        }

        if (filter.createdFromInstant() != null) {
            where.append(
                    " AND function('timezone', coalesce(d.createdTimeOffset, :defaultOffset), d.createdAt) >= :createdFrom ");
            params.put("defaultOffset", UserTimezoneUtil.DEFAULT_OFFSET);
            params.put("createdFrom", Timestamp.from(filter.createdFromInstant()));
        }

        if (filter.createdToInstantExclusive() != null) {
            where.append(
                    " AND function('timezone', coalesce(d.createdTimeOffset, :defaultOffset), d.createdAt) < :createdTo ");
            params.put("defaultOffset", UserTimezoneUtil.DEFAULT_OFFSET);
            params.put("createdTo", Timestamp.from(filter.createdToInstantExclusive()));
        }

        String fromClause = " FROM WalletDeposit d LEFT JOIN FETCH d.depositBank ";
        return new QueryParts(
                "SELECT DISTINCT d" + fromClause + where + " ORDER BY d.createdAt DESC",
                "SELECT COUNT(d) FROM WalletDeposit d" + where,
                params
        );
    }

    private void appendUserScope(StringBuilder where, Map<String, Object> params, DepositListFilter filter) {
        if (filter.hasActingScope()) {
            where.append(" AND (d.userId = :userId OR d.actingUserId = :actingUserId) ");
            params.put("userId", filter.userId());
            params.put("actingUserId", filter.actingUserId());
        } else if (filter.hasUserScope()) {
            where.append(" AND d.userId = :userId ");
            params.put("userId", filter.userId());
        }
    }

    private record QueryParts(String dataJpql, String countJpql, Map<String, Object> params) {}
}
