package com.aerionsoft.application.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtrasDTO {
    private Long id;
    private String seatCode;
    private String mealCode;
    private String baggageCode;
    private String flightNumber;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}

