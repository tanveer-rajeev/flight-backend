package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TopDestinationDto {
    private String airportCode;
    private String cityName;
    private String countryName;
    private Long searchCount;
    private Long bookingCount;
}

