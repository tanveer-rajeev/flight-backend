package com.aerionsoft.application.dto.booking;

import lombok.Data;

import java.util.List;

@Data
public class TravelInformation {

    private Long id;
    private String airlineName;
    private String flightNumber;

    private String origin;
    private String destination;
    private String departureAirport;
    private String arrivalAirport;
    private String departureDate;
    private String departureTime;
    private String arrivalDate;
    private String arrivalTime;

    private String fareBasis;
    private Integer quantity;
    private String currency;
    private Double baseFare;
    private Double equivalentBaseFare;
    private Double baggageKg;
    private Double tax;

    private String duration;
    private String ticketNumber;
    private String instructions;
    private String flightType;
    private String airlineCode;

    private int onewaySegmentStopCount;
    private int returnSegmentStopCount;

    // Flight segments
    private List<SegmentDTO> segments;
}
