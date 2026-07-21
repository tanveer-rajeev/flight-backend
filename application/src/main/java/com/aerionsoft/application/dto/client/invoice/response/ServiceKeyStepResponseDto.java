package com.aerionsoft.application.dto.client.invoice.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceKeyStepResponseDto {
    private Long id;
    private String serviceKey;
    private Integer step;
}
