package com.aerionsoft.application.dto.client.invoice;

import com.aerionsoft.application.enums.client.InvoiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class InvoiceItemDto {

    @NotNull(message = "Account head ID is required")
    private Long accountHeadId;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotBlank(message = "Title is required")
    private String title;

    private String document;

    @NotNull(message = "Please provide an invoice type (FLIGHT, VISA, TOUR, OTHER)")
    private InvoiceType invoiceType;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    @NotNull(message = "Sell price is required")
    private BigDecimal sellPrice;

    @NotNull(message = "Buy price is required")
    private BigDecimal buyPrice;

    @NotNull(message = "Step is required")
    private Integer step;

    private Map<String, String> customValues;
}
