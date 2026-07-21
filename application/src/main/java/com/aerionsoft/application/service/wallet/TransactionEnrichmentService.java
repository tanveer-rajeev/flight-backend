package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.wallet.SourceEnrichment;
import com.aerionsoft.application.entity.wallet.Transaction;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionEnrichmentService {

    private final List<TransactionSourceResolver> resolvers;

    public TransactionEnrichmentService(List<TransactionSourceResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public SourceEnrichment enrich(Transaction txn) {
        return enrich(txn, buildCaches(List.of(txn)));
    }

    public SourceEnrichment enrich(Transaction txn, Map<String, Map<Long, ?>> caches) {
        if (txn == null || txn.getSourceType() == null) {
            return SourceEnrichment.empty();
        }
        TransactionSourceResolver resolver = findResolver(txn.getSourceType());
        if (resolver == null) {
            return SourceEnrichment.empty();
        }
        Map<Long, ?> cache = caches.getOrDefault(txn.getSourceType(), Map.of());
        return resolver.enrich(txn, cache);
    }

    public Map<String, Map<Long, ?>> buildCaches(Collection<Transaction> transactions) {
        Map<String, List<Long>> idsByType = transactions.stream()
                .filter(t -> t.getSourceType() != null && t.getSourceId() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getSourceType,
                        Collectors.mapping(Transaction::getSourceId, Collectors.toList())));

        Map<String, Map<Long, ?>> caches = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : idsByType.entrySet()) {
            TransactionSourceResolver resolver = findResolver(entry.getKey());
            if (resolver != null) {
                caches.put(entry.getKey(), resolver.batchLoad(entry.getValue()));
            }
        }
        return caches;
    }

    private TransactionSourceResolver findResolver(String sourceType) {
        return resolvers.stream()
                .filter(r -> r.supports(sourceType))
                .findFirst()
                .orElse(null);
    }
}
