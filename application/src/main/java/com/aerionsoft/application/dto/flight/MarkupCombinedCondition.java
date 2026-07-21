package com.aerionsoft.application.dto.flight;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One allowed combination of route, airline, and/or booking code.
 * Only non-empty fields on this row are evaluated (AND within the row).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarkupCombinedCondition {
    /** Origin-destination pair, e.g. {@code DAC-DXB}. Comma-separated for multiple routes on one row. */
    private String route;
    /** Airline IATA code(s), comma-separated. */
    private String airlineCode;
    /** Booking / fare class code(s), comma-separated, e.g. {@code Y,M,B}. */
    @JsonAlias("cabinClass")
    private String bookingCode;

    public boolean hasAnyField() {
        return isNotBlank(route) || isNotBlank(airlineCode) || isNotBlank(bookingCode);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
