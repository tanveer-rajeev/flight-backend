package com.aerionsoft.application.dto.admin.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgencyUserDto {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Double balance;
    private Integer status;
    private String createdAt;
    private boolean isAgency;
    private String code;
    private String agentCode;
    private Long businessId = null;
    private Long motherUserId = null;
}
