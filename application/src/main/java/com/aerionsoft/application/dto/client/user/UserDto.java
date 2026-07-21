package com.aerionsoft.application.dto.client.user;


import com.aerionsoft.application.dto.admin.LoginHistoryDto;
import com.aerionsoft.application.dto.admin.summery.AgentInfoDto;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)

public class UserDto {

    private Long id;
    private String email;
    private String phoneNumber;
    private String fullName;
    private String address;
    private String profilePictureUrl;
    private String currency = "AED";
    private Double balance;
    private String passportNumber;
    private String passportExpiryDate;
    private boolean isVerified;
    private String createdAt;
    private String updatedAt;
    private String image;
    private LocalDate dob;
    private String nationality;
    private boolean isAgency = false;
    private boolean isDeleted = false;

    private String code;

    private Long parentUserId;

    private List<LoginHistoryDto> loginHistory;
    private AgentInfoDto info;

    private String ticketPreview;
    private String invoicePreview;
    private String moneyReceiptPreview;

    private String role;
    private boolean isChild = false;
    private boolean isMother = false;

    // Credit limit fields - shows how much negative balance is allowed
    private Double creditLimit;
    private Double availableBalance; // balance + creditLimit = total spending power

    private BusinessDto business;

}
