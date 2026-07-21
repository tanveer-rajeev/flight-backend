package com.aerionsoft.application.controller.booking;

import com.aerionsoft.application.controller.BaseController;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.dto.traveller.TravellerResponse;
import com.aerionsoft.application.service.booking.TravellerService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/travellers")
public class TravellerController  extends BaseController{

    @Autowired
    private TravellerService travellerService;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-traveller')") // admin or user
    public ResponseEntity<BaseResponse<TravellerResponse>> createTraveller(
            @Valid @RequestBody TravellerRequest req, Authentication auth) {
        TravellerResponse t = travellerService.createTraveller(req,getUserIdFromAuthentication());
        return ResponseEntity.ok(BaseResponse.ok("Traveller created", t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-traveller')") // admin or user
    public ResponseEntity<BaseResponse<TravellerResponse>> updateTraveller(
            @PathVariable Long id,
            @Valid @RequestBody TravellerRequest req) {
        TravellerResponse t = travellerService.updateTraveller(id, req);
        return ResponseEntity.ok(BaseResponse.ok("Traveller updated", t));
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-traveller')")
    public ResponseEntity<BaseResponse<Page<TravellerResponse>>> searchTravellers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {

        Long createdBy = getUserIdFromAuthentication();
        String provider = getProviderName(authentication);
        Page<TravellerResponse> travellerPage = travellerService.searchTravellers(query, page, size, createdBy, provider);
        return ResponseEntity.ok(BaseResponse.ok("Travellers fetched", travellerPage));
    }

    @GetMapping("/filter")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-traveller')")
    public ResponseEntity<BaseResponse<Page<TravellerResponse>>> searchTravellersWithFilters(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String passport,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long createdBy = getUserIdFromAuthentication();
        Page<TravellerResponse> travellerPage = travellerService.searchTravellersWithFilters(
                createdBy, firstName, lastName, mobile, passport, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Travellers filtered successfully", travellerPage));
    }
}
