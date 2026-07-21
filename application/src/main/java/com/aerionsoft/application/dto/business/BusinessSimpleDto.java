package com.aerionsoft.application.dto.business;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BusinessSimpleDto {
    private Long id;
    private String companyName;
    private String companyEmail;
    private String companyAddress;
    private String companyPhone;
    private String companyLogo;
    private Long motherUserId;
    private String motherUserFullName;
}
