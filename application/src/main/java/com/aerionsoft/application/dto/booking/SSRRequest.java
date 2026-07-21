package com.aerionsoft.application.dto.booking;

import lombok.Data;

@Data
public class SSRRequest {
    private  String[] mealCode;
    private  String[] baggageCode;
    private  String[] seatCode;
}
