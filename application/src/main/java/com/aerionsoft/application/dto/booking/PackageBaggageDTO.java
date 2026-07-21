package com.aerionsoft.application.dto.booking;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageBaggageDTO {
    private Long id;
    private String pax;
    private Double weight;
    private String unit;
    private Integer flightNumber;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
