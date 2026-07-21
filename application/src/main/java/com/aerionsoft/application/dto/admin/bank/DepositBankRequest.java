package com.aerionsoft.application.dto.admin.bank;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepositBankRequest {

    @NotBlank(message = "Bank name is required")
    @Size(max = 255, message = "Bank name must not exceed 255 characters")
    private String bankName;

    @NotBlank(message = "Account name is required")
    @Size(max = 255, message = "Account name must not exceed 255 characters")
    private String accountName;

    @NotBlank(message = "Account number is required")
    @Size(max = 64, message = "Account number must not exceed 64 characters")
    private String accountNumber;

    @Size(max = 64, message = "Routing number must not exceed 64 characters")
    private String routingNumber;

    @Size(max = 255, message = "Branch must not exceed 255 characters")
    private String branch;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. USD)")
    private String currency = "USD";

    private Boolean isActive;

    @DecimalMin(value = "0", message = "Opening balance cannot be negative")
    private BigDecimal openingBalance;
}
