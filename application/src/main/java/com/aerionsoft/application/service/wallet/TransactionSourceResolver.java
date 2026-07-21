package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.dto.wallet.SourceEnrichment;
import com.aerionsoft.application.entity.wallet.Transaction;

import java.util.Collection;
import java.util.Map;

public interface TransactionSourceResolver {

    boolean supports(String sourceType);

    Map<Long, ?> batchLoad(Collection<Long> sourceIds);

    SourceEnrichment enrich(Transaction txn, Map<Long, ?> batchCache);
}
