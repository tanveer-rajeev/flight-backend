package com.aerionsoft.application.dto.admin.client;

import com.aerionsoft.application.dto.admin.summery.AgentInfoDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgencyProfileDto {

    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "fullName cannot be blank")
    private String fullName;

    @NotBlank(message = "phoneNumber cannot be blank")
    private String phoneNumber;

    private String image;

    private Double balance;

    private String passportNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate passportExpiryDate;

    @NotBlank(message = "Address cannot be blank")
    private String address;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private String currency;

    private String nationality;

    private String agentCode;

    private AgentInfoDto agentInfo;

    private boolean isAgency;
}
