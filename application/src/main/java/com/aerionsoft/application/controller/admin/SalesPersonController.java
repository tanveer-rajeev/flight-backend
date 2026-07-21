package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.salesperson.SalesPersonDto;
import com.aerionsoft.application.dto.salesperson.SalesPersonResponseDto;
import com.aerionsoft.application.dto.salesperson.UpdateSalesPersonRequest;
import com.aerionsoft.application.service.business.SalesPersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/admin/sales-persons")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.isFullAdmin(authentication)")
public class SalesPersonController {

    private final SalesPersonService salesPersonService;

    @PostMapping
    public ResponseEntity<BaseResponse<Long>> createSalesPerson(@Valid @RequestBody SalesPersonDto request) {
        Long id = salesPersonService.createSalesPerson(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Sales person created successfully", id));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<SalesPersonResponseDto>>> getAllSalesPersons(
            @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(BaseResponse.ok("Sales persons fetched successfully",
                salesPersonService.getAllSalesPersons(currency)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<SalesPersonResponseDto>> getSalesPersonById(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok("Sales person fetched successfully",
                salesPersonService.getSalesPersonById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> updateSalesPerson(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSalesPersonRequest request) {
        salesPersonService.updateSalesPerson(id, request);
        return ResponseEntity.ok(BaseResponse.ok("Sales person updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> deleteSalesPerson(@PathVariable Long id) {
        salesPersonService.deleteSalesPerson(id);
        return ResponseEntity.ok(BaseResponse.ok("Sales person deleted successfully"));
    }
}
