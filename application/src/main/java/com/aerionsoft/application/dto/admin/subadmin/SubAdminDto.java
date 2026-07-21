package com.aerionsoft.application.dto.admin.subadmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubAdminDto {
    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Size(max = 100)
    @Email(message = "Email must be a valid email address")
    private String email;

    @Size(max = 15)
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 25, message = "Password must be between 6 and 25 characters")
    private String password;

    @Size(max = 255)
    private String address;

    @NotBlank(message = "Currency is required")
    private String currency;

    @Pattern(
            regexp = "^$|([^\\s]+(\\.(?i)(jpg|jpeg|png))$)",
            message = "Image filename must end with .jpg or .jpeg or .png"
    )
    private String image;

    private boolean isActive = false;
    private boolean isVerified = false;
}
