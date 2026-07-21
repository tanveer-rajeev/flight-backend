package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.group.TravelInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TravelInformationRepository extends JpaRepository<TravelInformation, Long> {

    TravelInformation findByBookingId(Long bookingId);

    List<TravelInformation> findByBookingIdIn(Collection<Long> bookingIds);
}
