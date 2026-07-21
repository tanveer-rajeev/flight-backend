package com.aerionsoft.application.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SegmentDTO {

    private Long id;

    private String baggagePieceCount;

    // Duration in minutes
    private Integer duration;

    // Nested objects
    private AirportDTO origin;
    private AirportDTO destination;
    private AirlineDTO airline;

    // Cabin and baggage
    private String cabinClass;
    private Integer noOfSeatAvailable;
    private String cabinBaggage;
    private String baggage;
    private String bookingCode;
    // Segment order
    private Integer segmentOrder;
    private String segmentType; // ONEWAY, RETURN

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AirportDTO {
        private Long id;
        private String airportCode;
        private String airportName;
        private String terminal;
        private String cityCode;
        private String cityName;
        private String countryCode;
        private String countryName;
        private String time; // DepTime or ArrTime
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AirlineDTO {
        private Long id;
        private String airlineCode;
        private String airlineName;
        private String flightNumber;
        private String fareClass;
        private String operatingCarrier;
    }
}

