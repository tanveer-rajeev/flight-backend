package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LastTenAgencies {

//    createdAt, agencyId, Balance, status, name, email, phone

    private String createdAt;
    private String agencyId;
    private String balance;
    private String status;
    private String name;
    private String email;
    private String phone;
    private String code;

}
