package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.group.SegmentAirport;
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
public interface SegmentAirportRepository extends JpaRepository<SegmentAirport, Long> {

    List<SegmentAirport> findBySegmentId(Long segmentId);

    List<SegmentAirport> findBySegmentIdIn(Collection<Long> segmentIds);

    Optional<SegmentAirport> findFirstBySegmentIdAndAirportTypeOrderByIdAsc(Long segmentId, String airportType);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SegmentAirport sa WHERE sa.segmentId = :segmentId")
    void deleteBySegmentId(@Param("segmentId") Long segmentId);

    // Top booked routes (origin-destination pairs) from booking segments
    @Query(value = "SELECT o.airport_code as origin_code, d.airport_code as dest_code, " +
           "o.city_name as origin_city, d.city_name as dest_city, " +
           "o.country_name as origin_country, d.country_name as dest_country, " +
           "COUNT(*) as booking_count " +
           "FROM segment_airport o " +
           "JOIN segment_airport d ON o.segment_id = d.segment_id " +
           "WHERE o.airport_type = 'ORIGIN' AND d.airport_type = 'DESTINATION' " +
           "GROUP BY o.airport_code, d.airport_code, o.city_name, d.city_name, o.country_name, d.country_name " +
           "ORDER BY booking_count DESC", nativeQuery = true)
    List<Object[]> findTopBookedRoutes(Pageable pageable);

    // Top booked destinations
    @Query(value = "SELECT airport_code, city_name, country_name, COUNT(*) as booking_count " +
           "FROM segment_airport " +
           "WHERE airport_type = 'DESTINATION' " +
           "GROUP BY airport_code, city_name, country_name " +
           "ORDER BY booking_count DESC", nativeQuery = true)
    List<Object[]> findTopBookedDestinations(Pageable pageable);
}

