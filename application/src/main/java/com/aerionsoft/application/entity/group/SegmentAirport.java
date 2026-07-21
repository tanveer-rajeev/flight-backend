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
@Table(name = "segment_airport")
public class SegmentAirport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "segment_id")
    private Long segmentId;

    @Column(name = "airport_type")
    private String airportType; // ORIGIN or DESTINATION

    @Column(name = "airport_code")
    private String airportCode;

    @Column(name = "airport_name")
    private String airportName;

    @Column(name = "terminal")
    private String terminal;

    @Column(name = "city_code")
    private String cityCode;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "country_name")
    private String countryName;

    @Column(name = "time")
    private String time; // DepTime or ArrTime
}

