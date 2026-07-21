package com.aerionsoft.application.dto.flight;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentSearchResponse {

    private String originCode;
    private String originCity;
    private String originCountry;

    private String destinationCode;
    private String destinationCity;
    private String destinationCountry;

    private String departureDate;
    private String returnDate;

    /** ONEWAY or ROUNDTRIP */
    private String tripType;

    /** ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST */
    private String cabinClass;

    private Integer adults;
    private Integer children;
    private Integer infants;

    private Integer resultCount;
    private LocalDateTime searchedAt;
}

