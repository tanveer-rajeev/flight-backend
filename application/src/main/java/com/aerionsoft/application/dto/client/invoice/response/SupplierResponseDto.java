package com.aerionsoft.application.dto.client.invoice.response;

import com.aerionsoft.application.dto.client.branch.BranchResponseDto;
import com.aerionsoft.application.dto.client.invoice.SupplierProviderMappingDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SupplierResponseDto {
    private Long id;
    private Long branchId;
    private BranchResponseDto branch;
    private String name;
    private String title;
    private String email;
    private String phoneNumber;
    private String address;
    private String description;
    private Boolean isDeleted;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal paidAMount;
    private BigDecimal payableAMount;
    private BigDecimal initialBalance;
    private List<SupplierProviderMappingDto> providerMappings;
}
