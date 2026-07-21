package com.aerionsoft.application.dto;

import com.aerionsoft.application.enums.business.BusinessStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBusinessStatusRequest {
    private BusinessStatus status;
}

