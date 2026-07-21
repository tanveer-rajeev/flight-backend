package com.aerionsoft.application.dto.wallet;

import com.aerionsoft.application.dto.ledger.LedgerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceEnrichment {
    private String sourceType;
    private Long sourceId;
    private String label;
    private String detail;
    private String status;
    private LedgerResponse.DepositInfo depositInfo;
    private LedgerResponse.BookingInfo bookingInfo;

    public static SourceEnrichment empty() {
        return SourceEnrichment.builder().build();
    }
}
