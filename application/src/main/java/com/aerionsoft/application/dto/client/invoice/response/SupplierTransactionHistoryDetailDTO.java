package com.aerionsoft.application.dto.client.invoice.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierTransactionHistoryDetailDTO {
    private Long id;
    private String key;
    private String value;
}

