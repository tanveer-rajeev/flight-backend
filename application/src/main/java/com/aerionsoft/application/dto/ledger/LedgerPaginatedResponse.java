package com.aerionsoft.application.dto.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerPaginatedResponse {
    /**
     * Net balance before the current filter window (start of {@code from} when set,
     * or before the oldest entry in the filtered set when {@code from} is not set).
     */
    private Double openingBalance;
    private Page<LedgerResponse> data;
}

