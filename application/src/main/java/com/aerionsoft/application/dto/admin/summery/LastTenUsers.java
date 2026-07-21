package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LastTenUsers {

// createdAt, userId, name, email,phone status
    private String createdAt;
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String status;
}
