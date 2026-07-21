package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivity {

    private List<LastTenAgencies> lastTenAgencies;
    private List<LastTenUsers> lastTenUsers;
    private List<LastTenBookings> lastTenBookings;
    private List<LastTenDeposits> lastTenDeposits;


}
