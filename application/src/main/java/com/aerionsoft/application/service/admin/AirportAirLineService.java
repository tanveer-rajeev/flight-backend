package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.admin.AirlineDto;
import com.aerionsoft.application.dto.admin.AirportDto;
import com.aerionsoft.application.entity.AirLine;
import com.aerionsoft.application.entity.Airport;
import com.aerionsoft.application.repository.common.AirlineRepository;
import com.aerionsoft.application.repository.common.AirportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AirportAirLineService {


    @Autowired
    AirportRepository airportRepository;
    @Autowired
    AirlineRepository airlineRepository;

    public List<AirportDto> getAllAirports() {
        return airportRepository.findAll()
                .stream()
                .map(this::mapToAirportDto)
                .toList();
    }



    @Cacheable(value = "airports", key = "#airport_query")
    public AirportDto getAirportsByAirline(String airport_query) {

        Optional<Airport> airportOpt = airportRepository.findByCode(airport_query);
        if (airportOpt.isPresent()) return mapToAirportDto(airportOpt.get());

        airportOpt = airportRepository.findByNameIgnoreCase(airport_query);
        if (airportOpt.isPresent()) return mapToAirportDto(airportOpt.get());

        airportOpt = airportRepository.findByCityNameIgnoreCase(airport_query);
        if (airportOpt.isPresent()) return mapToAirportDto(airportOpt.get());

        airportOpt = airportRepository.findByCityCodeIgnoreCase(airport_query);
        if (airportOpt.isPresent()) return mapToAirportDto(airportOpt.get());

        airportOpt = airportRepository.findByCountryCodeIgnoreCase(airport_query);
        if (airportOpt.isPresent()) return mapToAirportDto(airportOpt.get());

        airportOpt = airportRepository.findByCountryNameIgnoreCaseContaining(airport_query);
        return airportOpt.map(this::mapToAirportDto).orElse(null);

    }

    public void addAirport(AirportDto airportDto) {
        // Check if airport already exists
        if (airportRepository.findByCode(airportDto.getCode()).isPresent()) {
            throw ServiceExceptions.duplicate("Airport with code " + airportDto.getCode() + " already exists");
        }

        // Validate required fields
        if (airportDto.getCode() == null || airportDto.getName() == null) {
            throw new IllegalArgumentException("Airport code and name are required");
        }

        Airport airport = new Airport();
        airport.setCode(airportDto.getCode().toUpperCase());  // Standardize code format
        airport.setName(airportDto.getName());
        airport.setCityName(airportDto.getCityName());
        airport.setCityCode(airportDto.getCityCode());
        airport.setCountryCode(airportDto.getCountryCode());
        airport.setCountryName(airportDto.getCountryName());
        airport.setLat(airportDto.getLat());
        airport.setLon(airportDto.getLon());
        airport.setTimezone(airportDto.getTimezone());
        airport.setNumAirports(airportDto.getNumAirports());
        airport.setCity(airportDto.getCity());
        airport.setActiveSuggestion(airportDto.getActiveSuggestion());

        LocalDateTime now = UserDateTimeUtil.now();
        airport.setCreatedAt(now);
        airport.setUpdatedAt(now);

        airportRepository.save(airport);
    }

    public void updateAirport(AirportDto airportDto) {
        if (airportDto.getId() == null) {
            throw new IllegalArgumentException("Airport ID is required for update");
        }

        Airport existingAirport = airportRepository.findById(airportDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", airportDto.getId()));

        // Check if new code already exists for different airport
        if (!existingAirport.getCode().equals(airportDto.getCode()) &&
                airportRepository.findByCode(airportDto.getCode()).isPresent()) {
            throw ServiceExceptions.duplicate("Airport with code " + airportDto.getCode() + " already exists");
        }

        existingAirport.setCode(airportDto.getCode().toUpperCase());
        existingAirport.setName(airportDto.getName());
        existingAirport.setCityName(airportDto.getCityName());
        existingAirport.setCityCode(airportDto.getCityCode());
        existingAirport.setCountryCode(airportDto.getCountryCode());
        existingAirport.setCountryName(airportDto.getCountryName());
        existingAirport.setLat(airportDto.getLat());
        existingAirport.setLon(airportDto.getLon());
        existingAirport.setTimezone(airportDto.getTimezone());
        existingAirport.setNumAirports(airportDto.getNumAirports());
        existingAirport.setCity(airportDto.getCity());
        existingAirport.setActiveSuggestion(airportDto.getActiveSuggestion());
        existingAirport.setUpdatedAt(UserDateTimeUtil.now());

        airportRepository.save(existingAirport);
    }

    public void bulkAddAirports(List<AirportDto> airportDtos) {
        List<Airport> airports = new ArrayList<>();
        LocalDateTime now = UserDateTimeUtil.now();

        for (AirportDto dto : airportDtos) {
            Airport airport = new Airport();
            airport.setCode(dto.getCode());
            airport.setName(dto.getName());
            airport.setCityName(dto.getCityName());
            airport.setCityCode(dto.getCityCode());
            airport.setCountryCode(dto.getCountryCode());
            airport.setCountryName(dto.getCountryName());
            airport.setLat(dto.getLat());
            airport.setLon(dto.getLon());
            airport.setTimezone(dto.getTimezone());
            airport.setNumAirports(dto.getNumAirports());
            airport.setCity(dto.getCity());
            airport.setActiveSuggestion(dto.getActiveSuggestion());
            airport.setCreatedAt(now);
            airport.setUpdatedAt(now);
            airports.add(airport);

        }
        airportRepository.saveAll(airports);
    }

    public AirportDto mapToAirportDto(Airport airportData) {
        AirportDto dto = new AirportDto();
        dto.setId(airportData.getId());
        dto.setCode(airportData.getCode());
        dto.setName(airportData.getName());
        dto.setCityName(airportData.getCityName());
        dto.setCityCode(airportData.getCityCode());
        dto.setCountryCode(airportData.getCountryCode());
        dto.setCountryName(airportData.getCountryName());
        dto.setLat(airportData.getLat());
        dto.setLon(airportData.getLon());
        dto.setTimezone(airportData.getTimezone());
        dto.setNumAirports(airportData.getNumAirports());
        dto.setCity(airportData.getCity());
        dto.setActiveSuggestion(airportData.getActiveSuggestion());

        return dto;
    }

    public  List<AirlineDto> getAllAirlines() {
        return airlineRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::mapToAirlineDto)
                .toList();
    }

    @Cacheable(value = "airlineByCode", key = "#airline_query")
    public List<AirlineDto> getAirlinesByQuery(String airline_query) {
        // Try find by IATA
        List<AirLine> byIata = airlineRepository.findByIATA(airline_query);
        if (!byIata.isEmpty()) {
            return byIata.stream()
                    .map(this::mapToAirlineDto)
                    .collect(Collectors.toList());
        }

        // Try find by ICAO
        List<AirLine> byIcao = airlineRepository.findByICAOIgnoreCase(airline_query);
        if (!byIcao.isEmpty()) {
            return byIcao.stream()
                    .map(this::mapToAirlineDto)
                    .collect(Collectors.toList());
        }

        // Try find by Name
        List<AirLine> byName = airlineRepository.findByNameIgnoreCase(airline_query);
        if (!byName.isEmpty()) {
            return byName.stream()
                    .map(this::mapToAirlineDto)
                    .collect(Collectors.toList());
        }

        // If nothing found, return empty list or null
        return Collections.emptyList();
    }



    public void addAirline(AirlineDto airlineDto) {

        AirLine airLine = new AirLine();
        airLine.setFS(airlineDto.getFS());
        airLine.setIATA(airlineDto.getIATA());
        airLine.setICAO(airlineDto.getICAO());
        airLine.setName(airlineDto.getName());
        airLine.setActive(airlineDto.getActive());
        airLine.setIsDomestic(airlineDto.getIsDomestic());

        airlineRepository.save(airLine);
    }

    public void bulkAddAirlines(List<AirlineDto> airlineDtos) {
        List<AirLine> airlines = new ArrayList<>();

        for (AirlineDto dto : airlineDtos) {
            AirLine airLine = new AirLine();
            airLine.setFS(dto.getFS());
            airLine.setIATA(dto.getIATA());
            airLine.setICAO(dto.getICAO());
            airLine.setName(dto.getName());
            airLine.setActive(dto.getActive());
            airLine.setIsDomestic(dto.getIsDomestic());

            airlines.add(airLine);
        }

        airlineRepository.saveAll(airlines);
    }


    public void updateAirline(AirlineDto airlineDto) {
        if (airlineDto.getAirlineId() == null) {
            throw new IllegalArgumentException("Airline ID is required for update");
        }

        AirLine existingAirline = airlineRepository.findById(airlineDto.getAirlineId())
                .orElseThrow(() -> new ResourceNotFoundException("Airline", airlineDto.getAirlineId()));

        existingAirline.setFS(airlineDto.getFS());
        existingAirline.setIATA(airlineDto.getIATA());
        existingAirline.setICAO(airlineDto.getICAO());
        existingAirline.setName(airlineDto.getName());
        existingAirline.setActive(airlineDto.getActive());
        existingAirline.setIsDomestic(airlineDto.getIsDomestic());

        airlineRepository.save(existingAirline);
    }


    public AirlineDto mapToAirlineDto(AirLine airLine) {
        AirlineDto dto = new AirlineDto();
        dto.setAirlineId(airLine.getId());
        dto.setFS(airLine.getFS());
        dto.setIATA(airLine.getIATA());
        dto.setICAO(airLine.getICAO());
        dto.setName(airLine.getName());
        dto.setActive(airLine.getActive());
        dto.setIsDomestic(airLine.getIsDomestic());

        return dto;
    }

}
