package com.aerionsoft.application.dto.search;

import com.aerionsoft.application.dto.admin.client.AgencyUserDto;
import com.aerionsoft.application.dto.booking.BookingResponse;
import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSearchResponse {
    private List<BookingResponse> bookings;
    private List<AgencyUserDto> agents;
    private List<WalletDepositResponse> deposits;
    private String searchQuery;
    private Integer totalResults;
}

