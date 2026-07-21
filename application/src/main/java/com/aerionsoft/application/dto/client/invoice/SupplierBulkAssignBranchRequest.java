package com.aerionsoft.application.dto.client.invoice;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SupplierBulkAssignBranchRequest {

    /** Target branch. Omit or null to remove branch assignment from all listed suppliers. */
    private Long branchId;

    @NotEmpty(message = "At least one supplier id is required")
    private List<Long> supplierIds;
}
