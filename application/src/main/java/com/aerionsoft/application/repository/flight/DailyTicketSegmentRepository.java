package com.aerionsoft.application.repository.flight;

import com.aerionsoft.application.entity.DailyTicketSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface DailyTicketSegmentRepository extends JpaRepository<DailyTicketSegment, Long> {

    List<DailyTicketSegment> findByDateOrderByCreatedAtDesc(LocalDate date);

    List<DailyTicketSegment> findByDateAndChannelOrderByCreatedAtDesc(LocalDate date, String channel);

    @Query("SELECT d.pnr FROM DailyTicketSegment d WHERE d.date = :date AND d.channel = :channel")
    Set<String> findExistingPnrsByDateAndChannel(@Param("date") LocalDate date, @Param("channel") String channel);
}

