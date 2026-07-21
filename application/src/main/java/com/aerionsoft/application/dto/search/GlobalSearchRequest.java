
package com.aerionsoft.application.dto.search;

import com.aerionsoft.application.enums.booking.SearchType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSearchRequest {
    private String query; // The search term (PNR, agent code, or deposit reference)
    private SearchType type; // Filter type: ALL, BOOKING, AGENT, or DEPOSIT (defaults to ALL if null)
}

