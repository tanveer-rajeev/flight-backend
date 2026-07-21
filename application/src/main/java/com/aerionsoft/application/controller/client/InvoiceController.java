package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.client.invoice.InvoiceDto;
import com.aerionsoft.application.dto.client.invoice.response.InvoiceFullResponseDTO;
import com.aerionsoft.application.dto.client.invoice.response.InvoiceResponseDto;
import com.aerionsoft.application.service.client.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/invoices")
public class InvoiceController extends BaseController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Invoice List
     *
     * @param page           page number
     * @param size           required item per page
     * @param authentication authenticate user
     * @return response as JSON
     */
    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-manual-invoice')")
    public ResponseEntity<BaseResponse<List<InvoiceResponseDto>>> getInvoices(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        List<InvoiceResponseDto> invoices = invoiceService.getInvoices(provider, authUserId, page, size);

        return ResponseEntity.ok(BaseResponse.ok(invoices));
    }

    /**
     * Invoice show
     *
     * @param id             to get specific invoice
     * @param authentication authenticate user
     * @return response as JSON
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-manual-invoice')")
    public ResponseEntity<BaseResponse<InvoiceFullResponseDTO>> getInvoice(@PathVariable Long id, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        InvoiceFullResponseDTO invoice = invoiceService.getInvoice(provider, authUserId, id);

        return ResponseEntity.ok(BaseResponse.ok(invoice));
    }

    /**
     * Invoice create
     *
     * @param invoiceDto     invoiceDto invoice create request
     * @param authentication authenticate
     * @return response as JSON
     */
    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-manual-invoice')")
    public ResponseEntity<BaseResponse<?>> createInvoice(@Valid @RequestBody InvoiceDto invoiceDto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        invoiceService.createInvoice(provider, authUserId, invoiceDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created("Invoice created successfully", null));
    }

    /**
     * Invoice update
     *
     * @param invoiceDto     invoice update request
     * @param id             to update specific item
     * @param authentication authenticate
     * @return response as JSON
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-manual-invoice')")
    public ResponseEntity<BaseResponse<?>> updateInvoice(@Valid @RequestBody InvoiceDto invoiceDto, @PathVariable Long id, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        invoiceService.updateInvoice(provider, authUserId, id, invoiceDto);

        return ResponseEntity.ok(BaseResponse.ok("Invoice updated successfully"));
    }
}

