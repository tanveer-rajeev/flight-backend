package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.group.BookingSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSegmentRepository extends JpaRepository<BookingSegment, Long> {

    List<BookingSegment> findByTravelInformationId(Long travelInformationId);

    List<BookingSegment> findByTravelInformationIdOrderBySegmentOrderAsc(Long travelInformationId);

    void deleteByTravelInformationId(Long travelInformationId);
}

