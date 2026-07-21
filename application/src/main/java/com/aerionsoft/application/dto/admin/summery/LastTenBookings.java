package com.aerionsoft.application.dto.admin.summery;

import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.dto.common.TravellerShortDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LastTenBookings {
    private String createdAt;
    private Long bookingId;
    private String status;
    private TravellerShortDto traveller;
    private UserDto createdBy;
    private UserDto agencyUser;
}
