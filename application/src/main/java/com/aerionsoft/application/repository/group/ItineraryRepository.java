package com.aerionsoft.application.repository.group;

import com.aerionsoft.application.entity.tour.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    List<Itinerary> findByPackageIdOrderByDayNumber(Long packageId);

    void deleteByPackageId(Long packageId);

    long countByPackageId(Long packageId);
}
