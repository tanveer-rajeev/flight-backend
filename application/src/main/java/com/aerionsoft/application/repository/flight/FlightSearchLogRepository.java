package com.aerionsoft.application.repository.flight;

import com.aerionsoft.application.entity.FlightSearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightSearchLogRepository extends JpaRepository<FlightSearchLog, Long> {

    // Top searched routes (origin-destination pairs)
    @Query("SELECT f.originCode, f.destinationCode, f.originCity, f.destinationCity, COUNT(f) as searchCount " +
           "FROM FlightSearchLog f " +
           "GROUP BY f.originCode, f.destinationCode, f.originCity, f.destinationCity " +
           "ORDER BY searchCount DESC")
    List<Object[]> findTopSearchedRoutes(Pageable pageable);

    // Top searched routes in date range
    @Query("SELECT f.originCode, f.destinationCode, f.originCity, f.destinationCity, COUNT(f) as searchCount " +
           "FROM FlightSearchLog f " +
           "WHERE f.searchedAt >= :startDate AND f.searchedAt <= :endDate " +
           "GROUP BY f.originCode, f.destinationCode, f.originCity, f.destinationCity " +
           "ORDER BY searchCount DESC")
    List<Object[]> findTopSearchedRoutesInDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Top searched destinations
    @Query("SELECT f.destinationCode, f.destinationCity, f.destinationCountry, COUNT(f) as searchCount " +
           "FROM FlightSearchLog f " +
           "GROUP BY f.destinationCode, f.destinationCity, f.destinationCountry " +
           "ORDER BY searchCount DESC")
    List<Object[]> findTopSearchedDestinations(Pageable pageable);

    // Top searched destinations in date range
    @Query("SELECT f.destinationCode, f.destinationCity, f.destinationCountry, COUNT(f) as searchCount " +
           "FROM FlightSearchLog f " +
           "WHERE f.searchedAt >= :startDate AND f.searchedAt <= :endDate " +
           "GROUP BY f.destinationCode, f.destinationCity, f.destinationCountry " +
           "ORDER BY searchCount DESC")
    List<Object[]> findTopSearchedDestinationsInDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Count total searches
    Long countBySearchedAtAfter(LocalDateTime afterTime);

    // Recent searches for a specific user, newest first
    List<FlightSearchLog> findByUserIdOrderBySearchedAtDesc(Long userId, Pageable pageable);
}

