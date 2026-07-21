package com.aerionsoft.application.dto.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerPageResponse {
    /**
     * Net balance before the current filter window: before {@code from} (if set), or before the
     * oldest entry in the filtered set (when {@code from} is not set). Use with rows in
     * chronological order to compute running balance.
     */
    private Double openingBalance;
    private List<LedgerResponse> entries;
}
