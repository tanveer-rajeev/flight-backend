package com.aerionsoft.application.controller.visa;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.visa.VisaInfoRequest;
import com.aerionsoft.application.dto.visa.VisaInfoResponse;
import com.aerionsoft.application.service.admin.VisaInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/visa-info")
public class VisaInfoController {

    @Autowired
    private VisaInfoService visaInfoService;

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'create-visa-info')")
    public ResponseEntity<BaseResponse<VisaInfoResponse>> createVisaInfo(@Valid @RequestBody VisaInfoRequest request) {
        VisaInfoResponse visaInfo = visaInfoService.createVisaInfo(request);
        return ResponseEntity.ok(BaseResponse.ok(visaInfo, "Visa info created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<VisaInfoResponse>> getVisaInfo(@PathVariable Long id) {
        VisaInfoResponse visaInfo = visaInfoService.getVisaInfoById(id);
        return ResponseEntity.ok(BaseResponse.ok(visaInfo, "Visa info retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<VisaInfoResponse>>> getAllVisaInfo() {
        List<VisaInfoResponse> visaInfoList = visaInfoService.getAllVisaInfo();
        return ResponseEntity.ok(BaseResponse.ok(visaInfoList, "Visa info list retrieved successfully"));
    }

    @GetMapping("/country/{country}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-visa-info')") // admin or user
    public ResponseEntity<BaseResponse<List<VisaInfoResponse>>> getVisaInfoByCountry(@PathVariable String country) {
        List<VisaInfoResponse> visaInfoList = visaInfoService.getVisaInfoByCountry(country);
        return ResponseEntity.ok(BaseResponse.ok(visaInfoList, "Visa info for country retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'update-visa-info')")
    public ResponseEntity<BaseResponse<VisaInfoResponse>> updateVisaInfo(@PathVariable Long id, @Valid @RequestBody VisaInfoRequest request) {
        VisaInfoResponse visaInfo = visaInfoService.updateVisaInfo(id, request);
        return ResponseEntity.ok(BaseResponse.ok(visaInfo, "Visa info updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-visa-info')")
    public ResponseEntity<BaseResponse<Void>> deleteVisaInfo(@PathVariable Long id) {
        visaInfoService.deleteVisaInfo(id);
        return ResponseEntity.ok(BaseResponse.ok("Visa info deleted successfully"));
    }
}
