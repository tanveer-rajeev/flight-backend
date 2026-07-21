package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.client.invoice.LedgerDto;
import com.aerionsoft.application.dto.client.invoice.response.LedgerResponseDto;
import com.aerionsoft.application.service.client.InvoiceLedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("api/ledgers")
public class InvoiceLedgerController extends BaseController {
    private final InvoiceLedgerService ledgerService;

    public InvoiceLedgerController(InvoiceLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * Retrieves all ledgers.
     *
     * @return a response containing the list of all ledgers
     */
    @GetMapping
//    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-ledger')")
    public ResponseEntity<BaseResponse<List<LedgerResponseDto>>> getAllLedgers(Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        List<LedgerResponseDto> ledgers= ledgerService.getAllLedgers(provider, authUserId);

        return ResponseEntity.ok(BaseResponse.ok(ledgers));
    }

    /**
     * Creates a new ledger.
     *
     * @param ledgerDto the ledger data to create
     * @return a success message after creating the ledger
     */
    @PostMapping
//    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-ledger')")
    public ResponseEntity<BaseResponse<String>> createLedger(@Valid @RequestBody LedgerDto ledgerDto,Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        this.ledgerService.createLedger(provider, authUserId, ledgerDto);

        return ResponseEntity.ok(BaseResponse.ok("Ledger created successfully"));
    }

    /**
     * Show Ledger
     *
     * @param id to find Ledger
     * @return LedgerResponseDto
     */
    @GetMapping("/{id}")
//    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-ledger')")
    public ResponseEntity<BaseResponse<LedgerResponseDto>> getLedger(@PathVariable Long id, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        LedgerResponseDto ledger = ledgerService.getLedgerById(provider, authUserId, id);

        return ResponseEntity.ok(BaseResponse.ok(ledger));
    }

    /**
     * Update Ledger
     *
     * @param id to find Ledger
     * @param ledgerDto update LedgerDto
     * @return Response
     */
    @PutMapping("/{id}")
//    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-ledger')")
    public ResponseEntity<BaseResponse<String>> updateLedger(@PathVariable Long id, @Valid @RequestBody LedgerDto ledgerDto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        ledgerService.updateLedgerById(provider, authUserId, id, ledgerDto);

        return ResponseEntity.ok(BaseResponse.ok("Ledger updated successfully"));
    }
}
