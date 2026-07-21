package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TopRouteDto {
    private String originCode;
    private String destinationCode;
    private String originCity;
    private String destinationCity;
    private String originCountry;
    private String destinationCountry;
    private Long searchCount;
    private Long bookingCount;
}

