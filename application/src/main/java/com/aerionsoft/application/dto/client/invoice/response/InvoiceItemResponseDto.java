package com.aerionsoft.application.dto.client.invoice.response;

import com.aerionsoft.application.dto.SupplierDto;
import com.aerionsoft.application.dto.accounthead.AccountHeadDto;
import com.aerionsoft.application.enums.client.InvoiceType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class InvoiceItemResponseDto {
    private Long id;
    private String title;
    private AccountHeadDto accountHead;
    private SupplierDto supplier;
    private String document;
    private InvoiceType invoiceType;
    private Integer quantity;
    private BigDecimal sellPrice;
    private BigDecimal buyPrice;
    private Integer step;

    private Map<String, String> customValues;
}
