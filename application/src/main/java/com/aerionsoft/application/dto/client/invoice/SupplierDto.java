package com.aerionsoft.application.dto.client.invoice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
public class SupplierDto {
    private Long agencyId;

    @NotBlank(message = "Supplier name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String title;

    @NotBlank(message = "Supplier email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Supplier phone is required")
    @Size(max = 15)
    private String phoneNumber;

    @NotBlank(message = "Supplier address is required")
    @Size(max = 255)
    private String address;

    @Size(max = 1024)
    private String description;

    private Long branchId;

    /** Pre-system outstanding balance owed to the supplier (can be negative for advance/credit). */
    private BigDecimal initialBalance;

    /** Provider/channel pairs this supplier handles. Use {@code OTHERS} (no channel) for the default fallback supplier. */
    private List<SupplierProviderMappingDto> providerMappings;
}
