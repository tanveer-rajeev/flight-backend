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
public class TourPackageRequest {
    private String title;
    private String description;
    private String fullDescription;
    private String destinationCity;
    private String destinationCountry;
    private String mapLocation;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long fromId;
    private Long typeId;
    private Set<String> flags;
    private Set<Long> categoryIds;
    private List<MediaRequest> media;
    private List<PricingRequest> pricing;
    private List<PackageItemRequest> packageItems;
    private List<ItineraryRequest> itineraries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaRequest {
        private String fileUrl;
        private String fileType;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingRequest {
        private String category;
        private BigDecimal price;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageItemRequest {
        private String itemType;
        private String itemTitle;
        private String itemDescription;
        private Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryRequest {
        private Integer dayNumber;
        private String imageUrl;
        private String activity;
        private List<MealRequest> meals;
        private List<AccommodationRequest> accommodations;
        private List<TransportRequest> transports;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealRequest {
        private String mealType;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccommodationRequest {
        private String hotelName;
        private String roomType;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransportRequest {
        private String transportType;
        private String providerName;
        private String departureLocation;
        private String arrivalLocation;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private String notes;
    }
}
