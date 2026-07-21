package com.aerionsoft.application.dto.salesperson;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSalesPersonRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 100)
    @Email(message = "Email must be a valid email address")
    private String email;

    @Size(max = 15)
    private String phoneNumber;

    @Size(max = 255)
    private String address;

    private String currency;

    @Pattern(
            regexp = "^$|([^\\s]+(\\.(?i)(jpg|jpeg|png))$)",
            message = "Image filename must end with .jpg or .jpeg or .png"
    )
    private String image;
}
