package com.aerionsoft.application.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPackageListItemResponse {
    private Long id;
    private String title;
    private String image;
    private String destinationCity;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<PricingDetail> pricing;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingDetail {
        private String category;
        private BigDecimal price;
        private String currency;
    }
}

