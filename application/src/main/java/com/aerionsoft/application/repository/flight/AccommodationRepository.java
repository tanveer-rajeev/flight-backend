package com.aerionsoft.application.repository.flight;

import com.aerionsoft.application.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    List<Accommodation> findByItineraryId(Long itineraryId);

    void deleteByItineraryId(Long itineraryId);
}
