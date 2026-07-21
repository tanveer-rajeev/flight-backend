package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.entity.ApiKey;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.service.admin.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;


@RestController
@Validated
@RequestMapping("/api/admin/common")
public class CommonController {

    @Autowired
    private ApiKeyService apiKeyService;

    @GetMapping("/providers")
    public ResponseEntity<BaseResponse<List<String>>> getProviders() {
        List<String> providers = Arrays.stream(Provider.values())
                .map(Provider::name)
                .toList();
        return ResponseEntity.ok(BaseResponse.ok("Providers fetched successfully", providers));
    }

    @PostMapping("/generate-api-key")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'generate-api-key')")
    public ResponseEntity<BaseResponse<String>> generateApiKey(@RequestParam String description) {
        ApiKey key = apiKeyService.generateKey(description);
        return ResponseEntity.ok(BaseResponse.ok( key.getKey()));
    }
}