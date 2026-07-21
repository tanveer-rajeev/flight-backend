package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.Booking.Traveller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TravellerRepository extends JpaRepository<Traveller, Long> {

    Page<Traveller> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);

    // Example: Filter by nationality
    Page<Traveller> findByNationalityIgnoreCase(String nationality, Pageable pageable);

    // Example: Filter by mobile
    Page<Traveller> findByMobileContaining(String mobile, Pageable pageable);

    // New methods for filtering by createdBy
    Page<Traveller> findByFirstNameContainingIgnoreCaseAndCreatedBy(String firstName, Long createdBy, Pageable pageable);

    Page<Traveller> findByNationalityIgnoreCaseAndCreatedBy(String nationality, Long createdBy, Pageable pageable);

    Page<Traveller> findByMobileContainingAndCreatedBy(String mobile, Long createdBy, Pageable pageable);

    Page<Traveller> findByCreatedBy(Long createdBy, Pageable pageable);

    List<Traveller> findByCreatedBy(Long createdBy);

    Integer countByIdIn(Collection<Long> id);

    long countByCreatedBy(Long createdBy);

    // Search across multiple fields with OR condition
    @Query("SELECT t FROM Traveller t WHERE " +
            "LOWER(COALESCE(t.firstName,'')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(t.lastName,'')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(t.email,'')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(t.mobile,'')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(t.passportNo,'')) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Traveller> searchByQuery(@Param("query") String query, Pageable pageable);

    @Query("""
SELECT t FROM Traveller t
WHERE t.createdBy = :createdBy AND (
LOWER(COALESCE(t.firstName,'')) LIKE CONCAT('%', LOWER(:query), '%') OR
LOWER(COALESCE(t.lastName,'')) LIKE CONCAT('%', LOWER(:query), '%') OR
LOWER(COALESCE(t.email,'')) LIKE CONCAT('%', LOWER(:query), '%') OR
LOWER(COALESCE(t.mobile,'')) LIKE CONCAT('%', LOWER(:query), '%') OR
LOWER(COALESCE(t.passportNo,'')) LIKE CONCAT('%', LOWER(:query), '%')
)
""")
    Page<Traveller> searchByQueryAndCreatedBy(
            @Param("query") String query,
            @Param("createdBy") Long createdBy,
            Pageable pageable
    );

    // Advanced search with individual filters
    @Query("SELECT t FROM Traveller t WHERE " +
            "(:createdBy IS NULL OR t.createdBy = :createdBy) AND " +
            "(:firstName IS NULL OR LOWER(t.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
            "(:lastName IS NULL OR LOWER(t.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
            "(:mobile IS NULL OR LOWER(t.mobile) LIKE LOWER(CONCAT('%', :mobile, '%'))) AND " +
            "(:passportNo IS NULL OR LOWER(t.passportNo) LIKE LOWER(CONCAT('%', :passportNo, '%')))")
    Page<Traveller> searchWithFilters(
            @Param("createdBy") Long createdBy,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("mobile") String mobile,
            @Param("passportNo") String passportNo,
            Pageable pageable);

    java.util.Optional<Traveller> findFirstByCreatedByAndPassportNoIgnoreCase(Long createdBy, String passportNo);

    java.util.Optional<Traveller> findFirstByCreatedByAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDob(
            Long createdBy,
            String firstName,
            String lastName,
            java.time.LocalDate dob
    );
}