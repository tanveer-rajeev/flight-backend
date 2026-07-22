package com.aerionsoft.application.repository.booking;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    // We’ll use Specifications for advanced filtering!
    Long countByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, BookingStatus bookingStatus);

//    Long countByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, BookingStatus status);


    @Query("SELECT b FROM Booking b WHERE b.createdBy.id = :userId")
    List<Booking> findByCreatedBy(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdBy.id = :userId AND b.status = :status")
    Integer countByCreatedByAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status);

    @Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.createdBy.id = :userId GROUP BY b.status")
    List<Object[]> countBookingsByStatusForUser(@Param("userId") Long userId);

    // Find booking by ID with access control
    @Query("SELECT b FROM Booking b WHERE b.id = :id AND (b.createdBy.id = :userId OR :isAdmin = true)")
    Optional<Booking> findByIdWithAccess(@Param("id") Long id, @Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);

    // Find bookings by provider
    List<Booking> findByProviderName(Provider providerName);

    // Find bookings by booking class
    List<Booking> findByBookingClass(String bookingClass);

    // Find bookings containing specific traveller ID
    @Query("SELECT b FROM Booking b WHERE b.travellerIds LIKE %:travellerId%")
    List<Booking> findByTravellerIdsContaining(@Param("travellerId") String travellerId);

    // Add method to count bookings by user and date range for dashboard statistics
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdBy.id = :userId AND b.createdAt BETWEEN :start AND :end")
    long countByCreatedByAndCreatedAtBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Optional<Booking> findByPnr(String pnr);

    List<Booking> findByPnrIgnoreCaseOrderByCreatedAtDesc(String pnr);

    List<Booking> findByPnrContainingIgnoreCase(String pnr);

    // Count all bookings with PNR (not null and not empty)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.pnr IS NOT NULL AND b.pnr <> ''")
    Long countAllWithPnr();

    // Count bookings by booking type where PNR exists
    @Query("SELECT b.type, COUNT(b) FROM Booking b WHERE b.pnr IS NOT NULL AND b.pnr <> '' GROUP BY b.type")
    List<Object[]> countByBookingTypeWithPnr();


    // Count bookings with PNR status only
    Long countByStatus(BookingStatus status);

    @Query("""
            SELECT b.status, COUNT(b)
            FROM Booking b
            WHERE b.status IN :statuses
            GROUP BY b.status
            """)
    List<Object[]> countGroupedByStatus(@Param("statuses") Collection<BookingStatus> statuses);

    // Sabre/Galileo PNR bookings with a payment deadline for scheduled auto-cancel
    @Query("""
        SELECT b FROM Booking b
        WHERE b.providerName IN (
            com.aerionsoft.application.enums.booking.Provider.SABRE,
            com.aerionsoft.application.enums.booking.Provider.GALILEO
        )
        AND b.status = com.aerionsoft.application.enums.booking.BookingStatus.PNR
        AND b.lastPaymentDate IS NOT NULL
        AND b.pnr IS NOT NULL
        AND b.autoCancelFailureCount < 3
        """)
    List<Booking> findPnrBookingsWithPaymentDeadlineForAutoCancel();

    // LCC PNR bookings (status-only expiry cancel; no external API)
    @Query("""
        SELECT b FROM Booking b
        WHERE b.providerName IN (
            com.aerionsoft.application.enums.booking.Provider.USBANGLAAPI,
            com.aerionsoft.application.enums.booking.Provider.ARABIA,
            com.aerionsoft.application.enums.booking.Provider.FLYDUBAI
        )
        AND b.status = com.aerionsoft.application.enums.booking.BookingStatus.PNR
        AND b.lastPaymentDate IS NOT NULL
        AND b.pnr IS NOT NULL
        """)
    List<Booking> findLccPnrBookingsWithPaymentDeadlineForExpiryCancel();

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Booking b SET b.autoCancelFailureCount = b.autoCancelFailureCount + 1 WHERE b.id = :bookingId")
    void incrementAutoCancelFailureCount(@Param("bookingId") Long bookingId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Booking b SET b.autoCancelFailureCount = 0 WHERE b.id = :bookingId")
    void resetAutoCancelFailureCount(@Param("bookingId") Long bookingId);


    @Query("""
        SELECT b.status, COUNT(b)
        FROM Booking b
        WHERE b.createdAt BETWEEN :start AND :end
        GROUP BY b.status
    """)
    List<Object[]> countTodayBookingByStatus(LocalDateTime start, LocalDateTime end);

    long countByCreatedAtBetweenAndStatusAndSourceType(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore, BookingStatus status, String sourceType);
    boolean existsByPnrIgnoreCase(String pnr);

    boolean existsByPnrIgnoreCaseAndIdNot(String pnr, Long id);

    List<Booking> findByPnrIgnoreCaseInAndStatusIn(Collection<String> pnrs, Collection<BookingStatus> statuses);
}