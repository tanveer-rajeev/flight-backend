package com.aerionsoft.application.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPackageResponse {
    private Long id;
    private String title;
    private String description;
    private String fullDescription;
    private String destinationCity;
    private String destinationCountry;
    private String mapLocation;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long createdBy;
    private Long fromId;
    private Long searchCount;
    private Long viewCount;
    private Set<String> flags;
    private List<TourCategoryResponse> categories;
    private TourPackageTypeResponse type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MediaResponse> media;
    private List<PricingResponse> pricing;
    private List<PackageItemResponse> packageItems;
    private List<ItineraryResponse> itineraries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaResponse {
        private Long id;
        private String fileUrl;
        private String fileType;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingResponse {
        private Long id;
        private String category;
        private BigDecimal price;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageItemResponse {
        private Long id;
        private String itemType;
        private String itemTitle;
        private String itemDescription;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryResponse {
        private Long id;
        private Integer dayNumber;
        private String imageUrl;
        private String activity;
        private List<MealResponse> meals;
        private List<AccommodationResponse> accommodations;
        private List<TransportResponse> transports;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealResponse {
        private Long id;
        private String mealType;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccommodationResponse {
        private Long id;
        private String hotelName;
        private String roomType;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransportResponse {
        private Long id;
        private String transportType;
        private String providerName;
        private String departureLocation;
        private String arrivalLocation;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private String notes;
    }
}
