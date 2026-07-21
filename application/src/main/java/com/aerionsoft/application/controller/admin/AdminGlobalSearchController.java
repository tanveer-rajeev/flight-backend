package com.aerionsoft.application.controller.admin;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.search.GlobalSearchResponse;
import com.aerionsoft.application.enums.booking.SearchType;
import com.aerionsoft.application.service.common.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminGlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'global-search')")
    public ResponseEntity<BaseResponse<GlobalSearchResponse>> search(
            @RequestParam String query,
            @RequestParam(required = false) SearchType type) {
        GlobalSearchResponse response = globalSearchService.search(query, type);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }
}
