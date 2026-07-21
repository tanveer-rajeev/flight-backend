package com.aerionsoft.application.dto.client.user;

import lombok.Data;

@Data
public class PasswordChangeRequest {
    private  String oldPassword;
    private String newPassword;
}
