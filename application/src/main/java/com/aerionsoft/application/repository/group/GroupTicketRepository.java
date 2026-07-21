package com.aerionsoft.application.repository.group;

import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.entity.group.GroupTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GroupTicketRepository extends JpaRepository<GroupTicket, String>, JpaSpecificationExecutor<GroupTicket> {
    Page<GroupTicket> findByAirlineCodeAndStatus(
        String airlineCode,
        String status,
        Pageable pageable
    );
    
    Page<GroupTicket> findByAirlineCode(
        String airlineCode,
        Pageable pageable
    );
    
    Page<GroupTicket> findByStatus(
        String status,
        Pageable pageable
    );

    List<GroupTicket> findByDepartureDate(LocalDate departureDate);

    List<GroupTicket> findByDepartureDateAndOriginAndDestination(LocalDate departureDate,
                                                                String origin,
                                                                String destination);

    GroupTicket findByGfCode(String gfCode);

    GroupTicket findByGdsPnr(String gdsPnr);

    List<GroupTicket> findByDepartureDateAfterOrderByDepartureDateAsc(LocalDate departureDate);
    /**
     * Find all group tickets whose booking window has ended and that are not already expired.
     * Used by the auto-expiry scheduler.
     */
    List<GroupTicket> findByBookingEndsBeforeAndStatusNot(LocalDate date, String status);

    List<GroupTicket> findBySupplier(Supplier supplier);
}

