package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.admin.AirlineDto;
import com.aerionsoft.application.dto.admin.AirportDto;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.service.admin.AirportAirLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/admin/airport-airline")

public class AirportAirLineController {

    @Autowired
    AirportAirLineService airportAirLineService;

    // Add more methods as needed for your application
    @GetMapping("/airport/list")
    public ResponseEntity<BaseResponse<List<AirportDto>>> listAirports() {
        List<AirportDto> airports = airportAirLineService.getAllAirports();
        return ResponseEntity.ok(BaseResponse.ok(airports));
    }

    @PostMapping("/airport/create")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-airport')")
    public ResponseEntity<BaseResponse<Void>> crateAirport(@Valid @RequestBody AirportDto dto) {
        airportAirLineService.addAirport(dto);
        return ResponseEntity.ok(BaseResponse.ok("Airport  created successfully"));
    }

    @PutMapping("/airport/update")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-airport')")
    public ResponseEntity<BaseResponse<Void>> updateAirport(@Valid @RequestBody AirportDto dto) {
        airportAirLineService.updateAirport(dto);
        BaseResponse<Void> response = BaseResponse.ok("Airport updated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/airport-search")
    public ResponseEntity<BaseResponse<AirportDto>> getAirportsByAirline(@RequestParam(name = "query", required = true) String query) {
        AirportDto airport = new AirportDto();

        airport = airportAirLineService.getAirportsByAirline(query);
        if (airport == null) {
            throw new ResourceNotFoundException("Airport", query);
        }

        return ResponseEntity.ok( BaseResponse.ok("Airport fetched successfully", airport));
    }

    @PostMapping("/airport/bulk-create")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-airport')")
    public ResponseEntity<BaseResponse<Void>> crateAirportBulk(@Valid @RequestBody List<AirportDto> dto) {

        airportAirLineService.bulkAddAirports(dto);

        BaseResponse<Void> response = BaseResponse.ok("Airport or airline created successfully");
        return ResponseEntity.ok(response);
    }


    @GetMapping("/airline/list")
    public ResponseEntity<BaseResponse<List<AirlineDto>>> listAirlines() {
        List<AirlineDto> airlines = airportAirLineService.getAllAirlines();
        return ResponseEntity.ok(BaseResponse.ok(airlines));
    }

    @PostMapping("/airline/create")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-airline')")
    public ResponseEntity<BaseResponse<Void>> createAirline(@Valid @RequestBody AirlineDto dto) {
        airportAirLineService.addAirline(dto);
        return ResponseEntity.ok(BaseResponse.ok("Airline created successfully"));
    }

    @PutMapping("/airline/update")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-airline')")
    public ResponseEntity<BaseResponse<Void>> updateAirline(@Valid @RequestBody AirlineDto dto) {
        airportAirLineService.updateAirline(dto);
        return ResponseEntity.ok(BaseResponse.ok("Airline updated successfully"));
    }

    @GetMapping("/airline-search")
    public ResponseEntity<BaseResponse<List<AirlineDto>>> getAirlineByCode(@RequestParam(name = "query", required = true) String query) {
        List<AirlineDto> airlines = airportAirLineService.getAirlinesByQuery(query);
        if (airlines == null || airlines.isEmpty()) {
            throw new ResourceNotFoundException("Airline", query);
        }
        return ResponseEntity.ok(BaseResponse.ok("Airline(s) fetched successfully", airlines));
    }


    @PostMapping("/airline/bulk-create")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-airline')")
    public ResponseEntity<BaseResponse<Void>> createAirlineBulk(@Valid @RequestBody List<AirlineDto> dto) {
        airportAirLineService.bulkAddAirlines(dto);
        return ResponseEntity.ok(BaseResponse.ok("Airline created successfully"));
    }

}
