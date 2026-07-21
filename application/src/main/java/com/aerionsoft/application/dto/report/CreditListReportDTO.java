package com.aerionsoft.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditListReportDTO {

    /** Total number of matching businesses */
    private long totalCount;

    private Page<CreditListItemDTO> records;
}
