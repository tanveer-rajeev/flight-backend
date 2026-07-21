package com.aerionsoft.application.controller.common;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.CountriesDto;
import com.aerionsoft.application.entity.Countries;
import com.aerionsoft.application.service.common.CountriesService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@Validated
@RequestMapping("/api/countries")
public class CountriesController {

    @Autowired
    private CountriesService countriesService;

    @GetMapping("/list")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-countries')")
    public ResponseEntity<BaseResponse<List<Countries>>> list() {
        return ResponseEntity.ok(BaseResponse.ok(countriesService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-country')")
    public ResponseEntity<BaseResponse<Countries>> getById(@PathVariable Long id) {
        Optional<Countries> country = countriesService.getById(id);
        return country.map(countries -> ResponseEntity.ok(BaseResponse.ok(countries))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-country')") // admin and user

    public ResponseEntity<BaseResponse<Countries>> create(@Valid @RequestBody CountriesDto dto) {
        Countries country = countriesService.create(dto);
        return ResponseEntity.ok(BaseResponse.ok(country));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'edit-country')") // admin and user

    public ResponseEntity<BaseResponse<Countries>> update(@PathVariable Long id, @Valid @RequestBody CountriesDto dto) {
            Countries country = countriesService.update(id, dto);
            return ResponseEntity.ok(BaseResponse.ok(country));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-country')") // admin and user

    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id) {
        countriesService.delete(id);
        return ResponseEntity.ok(BaseResponse.ok("Country deleted successfully"));
    }

}
