package com.aerionsoft.application.dto.client.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceKeyStepDto {

    @NotBlank(message = "Service key is required")
    @Size(max = 55)
    private String serviceKey;

    @NotNull(message = "Step is required")
    private Integer step;
}
