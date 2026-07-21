package com.aerionsoft.application.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TourPackageSummaryResponse> tours;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TourPackageSummaryResponse {
        private Long id;
        private String title;
        private String destinationCity;
        private String destinationCountry;
        private String status;
    }
}
