package com.aerionsoft.application.repository.common;

import com.aerionsoft.application.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {

     public List<Airport> findByName(String name);
     public Optional<Airport> findByCode(String code);

     Optional<Airport> findByNameIgnoreCase(String name);

     Optional<Airport> findByCityNameIgnoreCase(String cityName);

     Optional<Airport> findByCityCodeIgnoreCase(String cityCode);

     Optional<Airport> findByCountryCodeIgnoreCase(String countryCode);

     Optional<Airport> findByCountryNameIgnoreCase(String countryName);
     Optional<Airport> findByCountryNameIgnoreCaseContaining(String countryNamePart);

}
