package com.aerionsoft.application.dto.ticketaction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-segment date/time update submitted when admin finalizes a REISSUE ticket action.
 * Times use the same ISO format as booking segments, e.g. {@code 2026-04-21T13:55}.
 */
@Getter
@Setter
public class ReissueSegmentDateUpdate {

    @NotNull
    private Integer segmentOrder;

    /** Origin departure time (maps to segment_airport.time for ORIGIN). */
    private String depTime;

    /** Destination arrival time (maps to segment_airport.time for DESTINATION). */
    private String arrTime;
}
