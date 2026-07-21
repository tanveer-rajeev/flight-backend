package com.aerionsoft.application.dto.flight;

import com.aerionsoft.application.dto.flight.validation.FareDesc;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetReservationResponse {

    @JsonProperty("tripType")
    private String tripType;

    @JsonProperty("providerName")
    private String providerName;

    @JsonProperty("originalPrice")
    private Double originalPrice;

    @JsonProperty("markupAmount")
    private Double markupAmount;

    @JsonProperty("taxAmount")
    private Double taxAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("itineraries")
    private List<ItineraryInfo> itineraries;

    @JsonProperty("segments")
    private List<SegmentInfo> segments;

    @JsonProperty("timeOffset")
    private String timeOffset;

    @JsonProperty("coreResponse")
    private CoreResponse coreResponse;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItineraryInfo {

        @JsonProperty("title")
        private String title;

        @JsonProperty("firstName")
        private String firstName;

        @JsonProperty("lastName")
        private String lastName;

        @JsonProperty("dob")
        private String dob;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("passportNo")
        private String passportNo;

        @JsonProperty("passportIssueDate")
        private String passportIssueDate;

        @JsonProperty("passportExpiryDate")
        private String passportExpiryDate;

        @JsonProperty("countryCode")
        private String countryCode;

        @JsonProperty("nationality")
        private String nationality;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SegmentInfo {

        @JsonProperty("unit")
        private Object unit;

        @JsonProperty("leg")
        private Integer leg;

        @JsonProperty("date")
        private Object date;

        @JsonProperty("rph")
        private Object rph;

        @JsonProperty("baggagePieceCount")
        private String baggagePieceCount;

        @JsonProperty("bookingCode")
        private String bookingCode;

        @JsonProperty("cabinBaggage")
        private String cabinBaggage;

        @JsonProperty("cabinClassName")
        private String cabinClassName;

        @JsonProperty("supplierFareClass")
        private String supplierFareClass;

        @JsonProperty("noOfSeatAvailable")
        private Integer noOfSeatAvailable;

        @JsonProperty("stopOver")
        private Object stopOver;

        @JsonProperty("craft")
        private Object craft;

        @JsonProperty("remark")
        private Object remark;

        @JsonProperty("duration")
        private Integer duration;

        @JsonProperty("origin")
        private EndpointInfo origin;

        @JsonProperty("destination")
        private EndpointInfo destination;

        @JsonProperty("baggage")
        private String baggage;

        @JsonProperty("cabinClass")
        private String cabinClass;

        @JsonProperty("airline")
        private AirlineInfo airline;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EndpointInfo {

        @JsonProperty("Airport")
        private AirportInfo airport;

        @JsonProperty("DepTime")
        private String depTime;

        @JsonProperty("ArrTime")
        private String arrTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
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

        @JsonProperty("TimeZoneInfoDetails")
        private Object timeZoneInfoDetails;

        @JsonProperty("UtcOffSet")
        private Object utcOffSet;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoreResponse {

        @JsonProperty("status")
        private String status;

        @JsonProperty("message")
        private String message;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("pnr")
        private String pnr;

        @JsonProperty("ticketNo")
        private String ticketNo;

        @JsonProperty("bookingDate")
        private String bookingDate;

        @JsonProperty("airline")
        private String airline;

        @JsonProperty("flightNo")
        private String flightNo;

        @JsonProperty("isPriceChanged")
        private Boolean isPriceChanged;

        @JsonProperty("oldPrice")
        private Double oldPrice;

        @JsonProperty("newPrice")
        private Double newPrice;

        @JsonProperty("currency")
        private String currency;
    }

    public FareDesc fareDesc;

}