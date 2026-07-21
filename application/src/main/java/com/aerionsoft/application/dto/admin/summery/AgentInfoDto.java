package com.aerionsoft.application.dto.admin.summery;

import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import com.aerionsoft.application.dto.booking.BookingResponse;
import com.aerionsoft.application.dto.traveller.TravellerResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentInfoDto {

    List<BookingResponse> bookings;
    List<TravellerResponse> travellers;
    HashMap<String,Integer> bookingStatuses;
    HashMap<String,Integer> TravellerStatuses;
    List<WalletDepositResponse> walletDeposits;

}
