package com.aerionsoft.application.entity.group;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "segment_airline")
public class SegmentAirline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "segment_id")
    private Long segmentId;

    @Column(name = "airline_code")
    private String airlineCode;

    @Column(name = "airline_name")
    private String airlineName;

    @Column(name = "flight_number")
    private String flightNumber;

    @Column(name = "fare_class")
    private String fareClass;

    @Column(name = "operating_carrier")
    private String operatingCarrier;
}

