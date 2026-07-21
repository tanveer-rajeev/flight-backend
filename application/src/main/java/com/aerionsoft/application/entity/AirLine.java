package com.aerionsoft.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "airlines")
public class AirLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long Id;

    @Column(name = "fs")
    private String FS;

    @Column(name = "iata")
    private String IATA;

    @Column(name = "icao")
    private String ICAO;

    @Column(name = "name")
    private String name;

    @Column(name = "active")
    private Integer active;

    @Column(name = "is_domestic")
    private Integer isDomestic;
}