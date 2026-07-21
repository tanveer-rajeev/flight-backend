package com.aerionsoft.application.repository.tour;

import com.aerionsoft.application.entity.tour.Transport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportRepository extends JpaRepository<Transport, Long> {

    List<Transport> findByItineraryId(Long itineraryId);

    List<Transport> findByItineraryIdAndTransportType(Long itineraryId, Transport.TransportType transportType);

    void deleteByItineraryId(Long itineraryId);
}
