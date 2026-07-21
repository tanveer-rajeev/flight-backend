package com.aerionsoft.application.dto.tour;

import lombok.Data;

import java.util.Map;

@Data
public class TourApplicationRequest {
    private Long tourId;
    private Map<String, Object> formResponses;
    private String status; // Use String for incoming request, convert to enum in service
    private String remarks;
}
