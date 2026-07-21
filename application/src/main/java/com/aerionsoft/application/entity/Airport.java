package com.aerionsoft.application.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;
    private String name;
    private String cityName;
    private String cityCode;
    private String countryCode;
    private String countryName;
    private String lat;
    private String lon;
    private String timezone;
    private Integer numAirports;
    private String city;
    private Integer activeSuggestion;

    private LocalDateTime createdAt ;
    private LocalDateTime updatedAt ;

}
