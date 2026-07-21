package com.aerionsoft.application.dto.admin.GroupTicket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightInfoDTO {
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private String departureTime;
    private String flightNumber;
    private Integer durationInMinutes;
    private Integer stops;
    private String equipment;
    private String arrivalDate;
    private String arrivalTime;
    private String originTerminal;
    private String destinationTerminal;

    /** 1-based leg index (connecting flights share the same leg). */
    private Integer leg;

    /** ONEWAY for outbound / multi-city legs; RETURN for return leg on round-trip. */
    private String segmentType;
}
