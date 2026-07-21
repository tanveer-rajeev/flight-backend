package com.aerionsoft.application.service.tour;

import com.aerionsoft.application.dto.tour.TourPackageListItemResponse;
import com.aerionsoft.application.dto.tour.TourPackageRequest;
import com.aerionsoft.application.dto.tour.TourPackageResponse;
import com.aerionsoft.application.dto.tour.TourPackageSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TourPackageService {

    TourPackageResponse createTourPackage(TourPackageRequest request, Long createdBy);

    TourPackageResponse updateTourPackage(Long id, TourPackageRequest request);

    TourPackageResponse getTourPackageById(Long id);

    Page<TourPackageResponse> getAllTourPackages(Pageable pageable);

    Page<TourPackageListItemResponse> getAllTourPackagesList(Pageable pageable);

    List<TourPackageResponse> getTourPackagesByStatus(String status);

    List<TourPackageResponse> getTourPackagesByDestination(String country, String city);

    List<TourPackageResponse> getTourPackagesByDateRange(LocalDate startDate, LocalDate endDate);

    TourPackageSearchResponse searchTourPackages(String keyword, Pageable pageable);

    Page<TourPackageResponse> searchPublishedTourPackages(String keyword, Pageable pageable);

    List<TourPackageResponse> getPopularPublishedTourPackages();

    List<TourPackageResponse> getTourPackagesByFlag(String flag);

    List<TourPackageResponse> getPublishedTourPackagesByFlag(String flag);

    void recordTourView(Long id);

    Long getTourViewCount(Long id);

    void deleteTourPackage(Long id);

    void changePackageStatus(Long id, String status);
}
