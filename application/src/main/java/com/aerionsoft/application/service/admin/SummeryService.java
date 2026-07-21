package com.aerionsoft.application.service.admin;
import com.aerionsoft.application.dto.admin.bank.TodayDepositsSummaryResponse;
import com.aerionsoft.application.dto.admin.summery.*;
import com.aerionsoft.application.service.booking.BookingService;
import com.aerionsoft.application.service.user.ActiveUserPresenceService;
import com.aerionsoft.application.service.user.UserService;
import com.aerionsoft.application.service.wallet.WalletService;
import com.aerionsoft.application.dto.common.UserShortDto;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.business.BusinessStatus;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.flight.FlightSearchLogRepository;
import com.aerionsoft.application.repository.booking.SegmentAirlineRepository;
import com.aerionsoft.application.repository.booking.SegmentAirportRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SummeryService {
    @Autowired
    private WalletService walletService;
    @Autowired
    private UserService userService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletDepositRepository depositRepo;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private SegmentAirportRepository segmentAirportRepository;

    @Autowired
    private SegmentAirlineRepository segmentAirlineRepository;

    @Autowired
    private FlightSearchLogRepository flightSearchLogRepository;

    @Autowired
    private ActiveUserPresenceService activeUserPresenceService;

    @Autowired
    private TimestampMapper timestampMapper;

    public HashMap<String, Object> getStatement() {
        HashMap<String, Object> map = new HashMap<>();

        HashMap<String, String> totalDeposits = getSumOfAllDepositsByDepositTypes();
        TodayDepositsSummaryResponse last7Days = walletService.getSumOfLast7DaysDepositsByDepositTypes();

        String totalAgencies = userService.countOfAgencies();
        String totalUsers = userService.countOfUsers();
        String last7DaysBookings = bookingService.last7DaysBookingsCount();
        String last7DaysIssuedCount = String.valueOf(bookingService.last7DaysIssuedCount());
        String last7DaysRefunds = bookingService.last7DaysRefundCount();

        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        long ticketed = bookingService.countPortalIssuedInUserDateRange(today.minusDays(6), today);

        map.put("totalDeposits", totalDeposits.get("totalDeposits"));
        map.put("totalApprovedDeposits", totalDeposits.get("totalApprovedDeposits"));
        map.put("last7DaysDeposits", last7Days.getTodayDeposits());
        map.put("last7DaysApprovedDeposits", last7Days.getTodayApprovedDeposits());
        map.put("last7DaysDepositsByCurrency", last7Days.getByCurrency());
        map.put("totalAgencies", totalAgencies);
        map.put("last7DaysBookings", last7DaysBookings);
        map.put("last7DaysTicketsIssued", last7DaysIssuedCount);
        map.put("last7DaysRefunds", last7DaysRefunds);
        map.put("last7DaysPortalIssued", ticketed);
        map.put("totalUsers", totalUsers);

        return map;
    }

    public HashMap<String, String> getSumOfAllDepositsByDepositTypes() {

        List<DepositType> depositTypes = List.of(
                DepositType.BANK_DEPOSIT,
                DepositType.BANK_TRANSFER_OR_MFS,
                DepositType.CHEQUE,
                DepositType.CASH
        );
        List<WalletDeposit> deposits = depositRepo.findByTypeIn(depositTypes);

        Double sum = deposits.stream()
                .filter(deposit -> deposit.getStatus() == DepositStatus.APPROVED)
                .mapToDouble(WalletDeposit::getAmount)
                .sum();

        long approvedCount = deposits.stream()
                .filter(deposit -> deposit.getStatus() == DepositStatus.APPROVED)
                .count();

        return new HashMap<>() {{
            put("totalDeposits", String.valueOf(sum));
            put("totalApprovedDeposits", String.valueOf(approvedCount));
        }};
    }


    public HashMap<String, Object> getTodayStatement() {
        HashMap<String, Object> map = new HashMap<>();

        TodayDepositsSummaryResponse todayDeposits = walletService.getSumOfTodayDepositsByDepositTypes();
        String todayAgencies = userService.countOfTodayAgencies();
        String todayUsers = userService.countOfTodayUsers();
        String todayBookings = bookingService.todayBookingsCount();
        String todayTicketsIssued = bookingService.todayIssueCount();
        String todayRefunds = bookingService.todayRefundCount();

        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        long todayPortal = bookingService.countPortalIssuedOnUserDate(today);

        map.put("todayDeposits", todayDeposits.getTodayDeposits());
        map.put("todayApprovedDeposits", todayDeposits.getTodayApprovedDeposits());
        map.put("depositsByCurrency", todayDeposits.getByCurrency());
        map.put("todayAgencies", todayAgencies);
        map.put("todayBookings", todayBookings);
        map.put("todayTicketsIssued", todayTicketsIssued);
        map.put("todayRefunds", todayRefunds);
        map.put("todayUsers", todayUsers);
        map.put("todayPortalIssued", todayPortal);
        return map;
    }


    public RecentActivity getRecentActivities() {

        List<LastTenBookings> lastTenBookings = bookingService.getLastTenBookings();
        List<LastTenAgencies> lastTenAgencies = userService.getLastTenAgencies();
        List<LastTenUsers> lastTenUsers = userService.getLastTenUsers();

        RecentActivity recentActivity = new RecentActivity();
        recentActivity.setLastTenBookings(lastTenBookings); //
        recentActivity.setLastTenAgencies(lastTenAgencies);
        recentActivity.setLastTenUsers(lastTenUsers);
        recentActivity.setLastTenDeposits(getLastTenDeposits());//

        return recentActivity;
    }


    public List<LastTenDeposits> getLastTenDeposits() {
        PageRequest pageRequest = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<WalletDeposit> deposits = depositRepo.findAll(pageRequest);

        return deposits.stream()
                .map(this::mapToLastTenDeposits)
                .collect(Collectors.toList());
    }

    private LastTenDeposits mapToLastTenDeposits(WalletDeposit walletDeposit) {
        LastTenDeposits dto = new LastTenDeposits();
        dto.setDepositId(walletDeposit.getId());
        dto.setStatus(walletDeposit.getStatus().toString());
        dto.setType(walletDeposit.getType().toString());
        dto.setAmount(walletDeposit.getAmount() != null ? walletDeposit.getAmount().toString() : "0.0");
        dto.setCreatedAt(timestampMapper.createdAtString(walletDeposit));

        Optional<User> user = userRepository.findById(walletDeposit.getUserId());

        if (user.isPresent()) {
            User u = user.get();

            if (u.getBusiness() != null && u.getBusiness().getId() != null) {
                dto.setAgencyUser(mapToUserShortDto(u));
            } else {
                dto.setUser(mapToUserShortDto(u));
            }
        }

        return dto;
    }

    private UserShortDto mapToUserShortDto(User user) {
        UserShortDto dto = new UserShortDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setCode(user.getCode());

        return dto;
    }

    public DashboardStatsResponse getDashboardStats() {
        // Count of bookings with PNR status only
        Long pnrStatusOnlyCount = bookingRepository.countByStatus(BookingStatus.PNR);

        // Count of pending deposit requests
        Long pendingDepositCount = depositRepo.countByStatus(DepositStatus.PENDING);

        // Count of new businesses (pending status)
        Long newBusinessCount = businessRepository.countByStatus(BusinessStatus.PENDING);

        return DashboardStatsResponse.builder()
                .pnrStatusOnlyCount(pnrStatusOnlyCount)
                .pendingDepositRequestCount(pendingDepositCount)
                .newBusinessCount(newBusinessCount)
                .build();
    }

    public ActiveUsersResponse getActiveUsers() {
        return activeUserPresenceService.getActiveUsers();
    }

    public HashMap<String, Long> getActiveUsersCount() {
        return activeUserPresenceService.getActiveUsersCount();
    }

    /**
     * Get top booked routes (origin-destination pairs)
     *
     * @param limit number of top routes to return
     * @return TopRoutesResponse containing list of top routes
     */
    public TopRoutesResponse getTopBookedRoutes(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Object[]> routeData = segmentAirportRepository.findTopBookedRoutes(pageRequest);

        List<TopRouteDto> topRoutes = new ArrayList<>();
        for (Object[] row : routeData) {
            TopRouteDto dto = TopRouteDto.builder()
                    .originCode(row[0] != null ? row[0].toString() : null)
                    .destinationCode(row[1] != null ? row[1].toString() : null)
                    .originCity(row[2] != null ? row[2].toString() : null)
                    .destinationCity(row[3] != null ? row[3].toString() : null)
                    .originCountry(row[4] != null ? row[4].toString() : null)
                    .destinationCountry(row[5] != null ? row[5].toString() : null)
                    .bookingCount(row[6] != null ? ((Number) row[6]).longValue() : 0L)
                    .searchCount(0L) // Will be populated if search logs exist
                    .build();
            topRoutes.add(dto);
        }

        return TopRoutesResponse.builder()
                .topRoutes(topRoutes)
                .totalRoutes((long) topRoutes.size())
                .build();
    }

    /**
     * Get top searched routes (origin-destination pairs)
     *
     * @param limit number of top routes to return
     * @return TopRoutesResponse containing list of top searched routes
     */
    public TopRoutesResponse getTopSearchedRoutes(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Object[]> routeData = flightSearchLogRepository.findTopSearchedRoutes(pageRequest);

        List<TopRouteDto> topRoutes = new ArrayList<>();
        for (Object[] row : routeData) {
            TopRouteDto dto = TopRouteDto.builder()
                    .originCode(row[0] != null ? row[0].toString() : null)
                    .destinationCode(row[1] != null ? row[1].toString() : null)
                    .originCity(row[2] != null ? row[2].toString() : null)
                    .destinationCity(row[3] != null ? row[3].toString() : null)
                    .searchCount(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                    .bookingCount(0L)
                    .build();
            topRoutes.add(dto);
        }

        return TopRoutesResponse.builder()
                .topRoutes(topRoutes)
                .totalRoutes((long) topRoutes.size())
                .build();
    }

    /**
     * Get top booked airlines
     *
     * @param limit number of top airlines to return
     * @return TopAirlinesResponse containing list of top airlines
     */
    public TopAirlinesResponse getTopBookedAirlines(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Object[]> airlineData = segmentAirlineRepository.findTopBookedAirlines(pageRequest);

        List<TopAirlineDto> topAirlines = new ArrayList<>();
        long totalBookings = 0L;

        for (Object[] row : airlineData) {
            long bookingCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            TopAirlineDto dto = TopAirlineDto.builder()
                    .airlineCode(row[0] != null ? row[0].toString() : null)
                    .airlineName(row[1] != null ? row[1].toString() : null)
                    .bookingCount(bookingCount)
                    .build();
            topAirlines.add(dto);
            totalBookings += bookingCount;
        }

        return TopAirlinesResponse.builder()
                .topAirlines(topAirlines)
                .totalBookings(totalBookings)
                .build();
    }

    /**
     * Get top booked destinations
     *
     * @param limit number of top destinations to return
     * @return TopDestinationsResponse containing list of top destinations
     */
    public TopDestinationsResponse getTopBookedDestinations(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Object[]> destData = segmentAirportRepository.findTopBookedDestinations(pageRequest);

        List<TopDestinationDto> topDestinations = new ArrayList<>();
        for (Object[] row : destData) {
            TopDestinationDto dto = TopDestinationDto.builder()
                    .airportCode(row[0] != null ? row[0].toString() : null)
                    .cityName(row[1] != null ? row[1].toString() : null)
                    .countryName(row[2] != null ? row[2].toString() : null)
                    .bookingCount(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                    .searchCount(0L)
                    .build();
            topDestinations.add(dto);
        }

        return TopDestinationsResponse.builder()
                .topDestinations(topDestinations)
                .totalDestinations((long) topDestinations.size())
                .build();
    }

    /**
     * Get top searched destinations
     *
     * @param limit number of top destinations to return
     * @return TopDestinationsResponse containing list of top searched destinations
     */
    public TopDestinationsResponse getTopSearchedDestinations(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Object[]> destData = flightSearchLogRepository.findTopSearchedDestinations(pageRequest);

        List<TopDestinationDto> topDestinations = new ArrayList<>();
        for (Object[] row : destData) {
            TopDestinationDto dto = TopDestinationDto.builder()
                    .airportCode(row[0] != null ? row[0].toString() : null)
                    .cityName(row[1] != null ? row[1].toString() : null)
                    .countryName(row[2] != null ? row[2].toString() : null)
                    .searchCount(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                    .bookingCount(0L)
                    .build();
            topDestinations.add(dto);
        }

        return TopDestinationsResponse.builder()
                .topDestinations(topDestinations)
                .totalDestinations((long) topDestinations.size())
                .build();
    }

}
