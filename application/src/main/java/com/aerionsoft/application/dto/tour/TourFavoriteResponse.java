package com.aerionsoft.application.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourFavoriteResponse {
    private Long id;
    private Long tourPackageId;
    private String tourTitle;
    private String destinationCity;
    private String destinationCountry;
    private String thumbnailUrl;
    private LocalDateTime favoritedAt;
}
