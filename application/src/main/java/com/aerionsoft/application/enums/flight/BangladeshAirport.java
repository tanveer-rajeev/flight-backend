package com.aerionsoft.application.enums.flight;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * IATA airport codes for Bangladesh domestic and international airports.
 * Used when GDS search responses omit {@code countryCode} on origin airports.
 */
@Getter
public enum BangladeshAirport {
    DAC("DAC", "Dhaka"),
    CGP("CGP", "Chittagong"),
    CTG("CTG", "Chittagong"),
    ZYL("ZYL", "Sylhet"),
    XYL("XYL", "Sylhet"),
    CXB("CXB", "Cox's Bazar"),
    JSR("JSR", "Jashore"),
    RJH("RJH", "Rajshahi"),
    SPD("SPD", "Saidpur"),
    BZL("BZL", "Barishal"),
    IRD("IRD", "Ishurdi");

    private static final Map<String, BangladeshAirport> BY_IATA = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                    airport -> airport.iataCode.toUpperCase(),
                    airport -> airport
            ));

    private final String iataCode;
    private final String cityName;

    BangladeshAirport(String iataCode, String cityName) {
        this.iataCode = iataCode;
        this.cityName = cityName;
    }

    public static Optional<BangladeshAirport> fromIataCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_IATA.get(code.trim().toUpperCase()));
    }

    public static boolean isBangladeshAirport(String code) {
        return fromIataCode(code).isPresent();
    }
}
