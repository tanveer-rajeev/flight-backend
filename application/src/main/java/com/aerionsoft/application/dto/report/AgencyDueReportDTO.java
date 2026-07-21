package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyDueReportDTO {

    /** Absolute sum of all mother-user balances matching the filter */
    private Double totalDueAmount;

    /** Total number of agencies matching the filter */
    private long totalCount;

    /**
     * Per-currency breakdown of the total amount.
     * Keys are currency codes (e.g. "USD", "AED").
     * For due reports the values are positive absolute amounts owed.
     * For credit reports the values are positive credit balances.
     */
    private Map<String, Double> totalsByCurrency;

    private Page<AgencyDueItemDTO> records;
}
