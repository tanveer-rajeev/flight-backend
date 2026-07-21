package com.aerionsoft.application.entity.group;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;



@Entity
@Table(name = "flight_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    /** 1-based leg index within the group ticket itinerary. */
    private Integer leg;

    /** Segment direction: ONEWAY or RETURN. */
    @Column(name = "segment_type", length = 20)
    private String segmentType;

}