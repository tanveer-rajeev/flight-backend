package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TopDestinationsResponse {
    private List<TopDestinationDto> topDestinations;
    private Long totalDestinations;
}

