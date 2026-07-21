package com.aerionsoft.application.dto.admin.subadmin.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubAdminResponseDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String image;
    private String currency;
    private String roleName;
    private boolean isActive;
    private boolean isVerified;
    private String createdDate;
}
