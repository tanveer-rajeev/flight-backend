package com.aerionsoft.application.repository.tour;

import com.aerionsoft.application.entity.tour.TourApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourApplicationRepository extends JpaRepository<TourApplication, Long>, JpaSpecificationExecutor<TourApplication> {

    Page<TourApplication> findAll(Pageable pageable);

    List<TourApplication> findByCreatedBy(String createdBy);

    long countByCreatedByAndSubmittedAtBetween(String createdBy, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
