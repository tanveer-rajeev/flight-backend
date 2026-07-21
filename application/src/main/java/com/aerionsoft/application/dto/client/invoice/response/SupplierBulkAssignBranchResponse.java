package com.aerionsoft.application.dto.client.invoice.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupplierBulkAssignBranchResponse {

    private Long branchId;
    private int updatedCount;
    private List<SupplierResponseDto> suppliers;
}
