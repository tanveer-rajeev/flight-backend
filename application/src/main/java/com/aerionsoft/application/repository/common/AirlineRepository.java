package com.aerionsoft.application.repository.common;

import com.aerionsoft.application.entity.AirLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AirlineRepository extends JpaRepository<AirLine, Long> {

    List<AirLine> findAllByOrderByIdAsc();

    List<AirLine> findByIATA(String iata);
    List<AirLine> findByICAOIgnoreCase(String icao);
    List<AirLine> findByNameIgnoreCase(String name);
}
