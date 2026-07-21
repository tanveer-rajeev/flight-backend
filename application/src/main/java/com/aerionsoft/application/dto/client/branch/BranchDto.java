package com.aerionsoft.application.dto.client.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchDto {

    @NotBlank(message = "Branch name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 1024)
    private String description;

    @Size(max = 255)
    private String address;

    @Size(max = 15)
    private String phoneNumber;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. USD)")
    private String currency;

    private Boolean isActive;
}
