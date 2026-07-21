package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.group.SegmentAirline;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SegmentAirlineRepository extends JpaRepository<SegmentAirline, Long> {

    List<SegmentAirline> findBySegmentIdIn(Collection<Long> segmentIds);

    Optional<SegmentAirline> findFirstBySegmentIdOrderByIdAsc(Long segmentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SegmentAirline sa WHERE sa.segmentId = :segmentId")
    void deleteBySegmentId(@Param("segmentId") Long segmentId);

    // Top booked airlines
    @Query(value = "SELECT airline_code, airline_name, COUNT(*) as booking_count " +
           "FROM segment_airline " +
           "WHERE airline_code IS NOT NULL AND airline_code != '' " +
           "GROUP BY airline_code, airline_name " +
           "ORDER BY booking_count DESC", nativeQuery = true)
    List<Object[]> findTopBookedAirlines(Pageable pageable);
}

