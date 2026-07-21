package com.aerionsoft.application.controller.client;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.client.invoice.InvoiceDynamicServiceDto;
import com.aerionsoft.application.dto.client.invoice.response.ServiceKeyStepResponseDto;
import com.aerionsoft.application.service.client.InvoiceDynamicService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("api/invoice-dynamic-services")
public class InvoiceDynamicServiceController extends BaseController {

    private final InvoiceDynamicService invoiceDynamicService;

    public InvoiceDynamicServiceController(InvoiceDynamicService invoiceDynamicService) {
        this.invoiceDynamicService = invoiceDynamicService;
    }

    /**
     * List of Dynamic Service
     *
     * @param authentication auth user
     * @return response as JSON
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Map<String, List<ServiceKeyStepResponseDto>>>> getDynamicServices(Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        Map<String, List<ServiceKeyStepResponseDto>> dynamicServices = invoiceDynamicService.getDynamicServicesGrouped(provider, authUserId);

         return ResponseEntity.ok(BaseResponse.ok(dynamicServices));
    }

    @GetMapping("/{serviceType}")
    public ResponseEntity<BaseResponse<List<ServiceKeyStepResponseDto>>> getDynamicServiceByServiceKey(@PathVariable String serviceType, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        List<ServiceKeyStepResponseDto> dynamicService = invoiceDynamicService.getDynamicServiceByServiceKey(provider, authUserId, serviceType);

        return ResponseEntity.ok(BaseResponse.ok(dynamicService));
    }

    /**
     * Store DynamicService
     *
     * @param invoiceDynamicServiceDto dynamic service create request
     * @param authentication auth user
     * @return response as JSON
     */
    @PostMapping
    public ResponseEntity<BaseResponse<?>> createInvoiceDynamicService(@Valid @RequestBody InvoiceDynamicServiceDto invoiceDynamicServiceDto, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        invoiceDynamicService.createDynamicService(provider, authUserId, invoiceDynamicServiceDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created("Dynamic service created successfully", null));
    }

    /**
     * Delete Dynamic Service Item
     *
     * @param id item to delete
     * @param authentication auth user
     * @return response as JSON
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<String>> deleteInvoiceDynamicService(@PathVariable Long id, Authentication authentication) {
        String provider = getProviderName(authentication);
        Long authUserId = getUserIdFromAuthentication();

        invoiceDynamicService.deleteDynamicService(provider, authUserId, id);

        return ResponseEntity.ok(BaseResponse.ok("Item deleted successfully"));
    }

}
