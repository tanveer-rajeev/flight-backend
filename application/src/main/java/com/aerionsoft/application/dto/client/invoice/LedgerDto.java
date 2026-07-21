package com.aerionsoft.application.dto.client.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LedgerDto {
    @Size(max = 100)
    private String title;

    private String image;

    @NotBlank(message = "Description is required")
    @Size(max = 1024)
    private String description;
}
