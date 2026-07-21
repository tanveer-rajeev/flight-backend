package com.aerionsoft.application.dto.gateway;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    private String pan = "4111111111111111";
    private String expiry = "2029-04";
    private String cvv = "123";
    private String cardholderName = "Jane Doe";
}
