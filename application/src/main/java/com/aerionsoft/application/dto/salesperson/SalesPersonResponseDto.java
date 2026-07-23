package com.aerionsoft.application.dto.salesperson;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalesPersonResponseDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String image;
}
