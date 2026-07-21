package com.aerionsoft.application.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for segment data matching the JSON structure from frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SegmentRequest {

    private String baggagePieceCount;
    private OriginInfo origin;
    private Integer duration;
    private DestinationInfo destination;
    private AirlineInfo airline;
    private String cabinClass;
    private Integer noOfSeatAvailable;
    private String cabinBaggage;
    private String baggage;
    private String bookingCode;
    private Integer segmentOrder;
    private String segmentType; // ONEWAY, RETURN

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OriginInfo {
        @JsonProperty("Airport")
        private AirportInfo airport;

        @JsonProperty("DepTime")
        private String depTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DestinationInfo {
        @JsonProperty("Airport")
        private AirportInfo airport;

        @JsonProperty("ArrTime")
        private String arrTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AirportInfo {
        @JsonProperty("AirportCode")
        private String airportCode;

        @JsonProperty("AirportName")
        private String airportName;

        @JsonProperty("Terminal")
        private String terminal;

        @JsonProperty("CityCode")
        private String cityCode;

        @JsonProperty("CityName")
        private String cityName;

        @JsonProperty("CountryCode")
        private String countryCode;

        @JsonProperty("CountryName")
        private String countryName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AirlineInfo {
        @JsonProperty("AirlineCode")
        private String airlineCode;

        @JsonProperty("AirlineName")
        private String airlineName;

        @JsonProperty("FlightNumber")
        private String flightNumber;

        @JsonProperty("FareClass")
        private String fareClass;

        @JsonProperty("OperatingCarrier")
        private String operatingCarrier;
    }
}
