package com.aerionsoft.application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "flight_search_log")
public class FlightSearchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_code")
    private String originCode;

    @Column(name = "origin_city")
    private String originCity;

    @Column(name = "origin_country")
    private String originCountry;

    @Column(name = "destination_code")
    private String destinationCode;

    @Column(name = "destination_city")
    private String destinationCity;

    @Column(name = "destination_country")
    private String destinationCountry;

    @Column(name = "departure_date")
    private String departureDate;

    @Column(name = "return_date")
    private String returnDate;

    @Column(name = "trip_type")
    private String tripType;

    @Column(name = "cabin_class")
    private String cabinClass;

    @Column(name = "adults")
    private Integer adults;

    @Column(name = "children")
    private Integer children;

    @Column(name = "infants")
    private Integer infants;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_ip")
    private String userIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "searched_at")
    private LocalDateTime searchedAt;

    @Column(name = "result_count")
    private Integer resultCount;
}

