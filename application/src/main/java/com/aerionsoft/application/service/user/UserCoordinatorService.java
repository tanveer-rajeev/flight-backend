package com.aerionsoft.application.service.user;
import com.aerionsoft.application.service.booking.TravellerService;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import com.aerionsoft.application.dto.admin.summery.AgentInfoDto;
import com.aerionsoft.application.dto.booking.BookingResponse;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.dto.traveller.TravellerResponse;
import com.aerionsoft.application.interafces.BookingInterface;
import com.aerionsoft.application.interafces.UserInterface;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserCoordinatorService {


    private final UserInterface userInterface;
    private  final BookingInterface bookingService;
    private final TravellerService travellerService;

    public UserCoordinatorService(UserInterface userInterface, BookingInterface bookingService,
                                  TravellerService travellerService) {
        this.travellerService = travellerService;
        this.userInterface = userInterface;
        this.bookingService = bookingService;
    }




    public UserDto getAgentInfo(Long userId, boolean isAgencyRequired) {

        UserDto userDtoAgency = userInterface.getUserById(userId);

        if (isAgencyRequired) {
            if (userDtoAgency == null || !userDtoAgency.isAgency()) {
                throw ServiceExceptions.notFound("User is not an agency or does not exist");
            }
        }

        AgentInfoDto agentInfo = new AgentInfoDto();

        List<BookingResponse> bookings = bookingService.getAllBookingsByUserId(userId);
        List<TravellerResponse> travellers = travellerService.getTravellerByCreatedBy(userId);
        HashMap<String, Integer> bookingCountByStatus = bookingService.getBookingCountByStatus(userId);

        HashMap<String, Integer> travellerCountByStatus = new HashMap<>();
        List<WalletDepositResponse> walletDeposits = userInterface.getAllDepositsByAgency(userId);

        HashMap<String, Collection<Long>> travellerIdsByStatus = bookingService.getTravellerIdsByStatus(userId);

        for (Map.Entry<String, Collection<Long>> entry : travellerIdsByStatus.entrySet()) {
            String status = entry.getKey();
            Collection<Long> travellerIds = entry.getValue();
            if (travellerIds != null && !travellerIds.isEmpty()) {
                Integer count = travellerService.getTravellerByIds(travellerIds);
                travellerCountByStatus.put(status, count);
            } else {
                travellerCountByStatus.put(status, 0);
            }
        }

        agentInfo.setBookingStatuses(bookingCountByStatus);
        agentInfo.setTravellerStatuses(travellerCountByStatus);
        agentInfo.setBookings(bookings);
        agentInfo.setTravellers(travellers);
        agentInfo.setWalletDeposits(walletDeposits);


        userDtoAgency.setInfo(agentInfo);

        return userDtoAgency;
    }

}
