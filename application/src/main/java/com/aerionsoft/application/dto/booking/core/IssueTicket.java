package com.aerionsoft.application.dto.booking.core;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IssueTicket {
    @NotBlank(message = "pnr is required")
    public String pnr;
    @NotBlank(message = "providerName is required")
    public String providerName;
    public String remarks;
    @NotBlank(message = "price_change_accepted is required")
    public String price_change_accepted;

}
