package com.aerionsoft.application.repository.tour;

import com.aerionsoft.application.entity.tour.TourPackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourPackageTypeRepository extends JpaRepository<TourPackageType, Long> {

    List<TourPackageType> findByIsActiveTrueOrderByNameAsc();
}
