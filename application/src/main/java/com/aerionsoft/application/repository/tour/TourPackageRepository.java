package com.aerionsoft.application.repository.tour;

import com.aerionsoft.application.entity.tour.TourCategory;
import com.aerionsoft.application.entity.tour.TourPackage;
import com.aerionsoft.application.enums.tour.TourFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TourPackageRepository extends JpaRepository<TourPackage, Long>, JpaSpecificationExecutor<TourPackage> {

    List<TourPackage> findByStatus(TourPackage.PackageStatus status);

    Page<TourPackage> findByStatus(TourPackage.PackageStatus status, Pageable pageable);

    List<TourPackage> findByDestinationCountryIgnoreCase(String country);

    List<TourPackage> findByDestinationCityIgnoreCase(String city);

    @Query("SELECT tp FROM TourPackage tp WHERE tp.startDate >= :startDate AND tp.endDate <= :endDate")
    List<TourPackage> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<TourPackage> findByCreatedBy(Long createdBy);

    List<TourPackage> findTop3BySearchCountGreaterThanOrderBySearchCountDesc(long searchCount);

    List<TourPackage> findTop3ByStatusAndSearchCountGreaterThanOrderBySearchCountDesc(
            TourPackage.PackageStatus status,
            long searchCount
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TourPackage tp SET tp.searchCount = tp.searchCount + 1 WHERE tp.id IN :ids")
    void incrementSearchCount(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TourPackage tp SET tp.viewCount = tp.viewCount + 1 WHERE tp.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT tp.viewCount FROM TourPackage tp WHERE tp.id = :id")
    Long getViewCount(@Param("id") Long id);

    List<TourPackage> findByFlagsContaining(TourFlag flag);

    List<TourPackage> findByStatusAndFlagsContaining(TourPackage.PackageStatus status, TourFlag flag);

    List<TourPackage> findByCategoriesContaining(TourCategory category);

    long countByType_Id(Long typeId);
}
