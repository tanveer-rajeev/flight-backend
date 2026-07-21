package com.aerionsoft.application.dto.flight.search.v1;

import lombok.Data;
import java.util.List;

@Data
public class SearchRequestV1 {
    private List<OriginDestination> originDestinations;
    private int adults = 0;
    private int children = 0;
    private int infants = 0;
    private int cabinClass = 1;
    /** Allowed providers for this search. Null means all providers. */
    private List<String> providers;
}

