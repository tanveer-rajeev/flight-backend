package com.aerionsoft.application.dto.client.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class InvoiceDynamicServiceDto {

    @NotBlank(message = "Service type is required")
    @Size(max = 55)
    private String serviceType;

    @NotEmpty(message = "Keys list cannot be empty")
    private List<ServiceKeyStepDto> keys;
}
