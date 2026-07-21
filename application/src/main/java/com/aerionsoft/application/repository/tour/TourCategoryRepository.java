package com.aerionsoft.application.repository.tour;

import com.aerionsoft.application.entity.tour.TourCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourCategoryRepository extends JpaRepository<TourCategory, Long> {

    List<TourCategory> findByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
