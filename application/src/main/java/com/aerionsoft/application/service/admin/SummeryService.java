package com.aerionsoft.application.service.admin;
import com.aerionsoft.application.dto.admin.bank.TodayDepositsSummaryResponse;
import com.aerionsoft.application.dto.admin.summery.*;
import com.aerionsoft.application.enums.booking.TicketActionStatus;
import com.aerionsoft.application.enums.booking.TicketActionType;
import com.aerionsoft.application.repository.booking.TicketActionRequestRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SummeryService {

    private static final List<TicketActionStatus> OPEN_TICKET_ACTION_STATUSES = List.of(
            TicketActionStatus.SUBMITTED,
            TicketActionStatus.QUOTED,
            TicketActionStatus.USER_CONFIRMED,
            TicketActionStatus.ADMIN_PROCESSING
    );

    private static final List<BookingStatus> DASHBOARD_BOOKING_STATUSES = List.of(
            BookingStatus.PROCESS,
            BookingStatus.PNR,
            BookingStatus.ON_HOLD,
            BookingStatus.BOOK,
            BookingStatus.CONFIRMED,
            BookingStatus.VALIDATION_PROCESS,
            BookingStatus.VALIDATION_PRICE_CHANGED,
            BookingStatus.REPRICE
    );

    private static final List<BookingStatus> BOOKING_NEEDS_ADMIN_ACTION_STATUSES = List.of(
            BookingStatus.PROCESS,
            BookingStatus.PNR,
            BookingStatus.ON_HOLD,
            BookingStatus.BOOK,
            BookingStatus.VALIDATION_PROCESS,
            BookingStatus.VALIDATION_PRICE_CHANGED,
            BookingStatus.REPRICE
    );

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
    private TicketActionRequestRepository ticketActionRequestRepository;

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
        Long pnrStatusOnlyCount = bookingRepository.countByStatus(BookingStatus.PNR);
        Long pendingDepositCount = depositRepo.countByStatus(DepositStatus.PENDING);
        Long newBusinessCount = businessRepository.countByStatus(BusinessStatus.PENDING);

        BookingStatusPendingStats bookings = buildBookingStatusPendingStats();
        TicketActionPendingStats ticketActions = buildTicketActionPendingStats();
        long ticketActionNeedsAdmin = ticketActions.getTotals().getNeedsAdminAction();

        long totalPendingItems = safe(newBusinessCount)
                + safe(pendingDepositCount)
                + safe(bookings.getNeedsAdminAction())
                + ticketActionNeedsAdmin;

        return DashboardStatsResponse.builder()
                .pnrStatusOnlyCount(pnrStatusOnlyCount)
                .pendingDepositRequestCount(pendingDepositCount)
                .newBusinessCount(newBusinessCount)
                .agencies(AdminPendingQueueStats.of(safe(newBusinessCount)))
                .deposits(AdminPendingQueueStats.of(safe(pendingDepositCount)))
                .bookings(bookings)
                .ticketActions(ticketActions)
                .summary(AdminPendingSummary.builder().totalPendingItems(totalPendingItems).build())
                .build();
    }

    private BookingStatusPendingStats buildBookingStatusPendingStats() {
        EnumMap<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : DASHBOARD_BOOKING_STATUSES) {
            counts.put(status, 0L);
        }

        for (Object[] row : bookingRepository.countGroupedByStatus(DASHBOARD_BOOKING_STATUSES)) {
            BookingStatus status = (BookingStatus) row[0];
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            counts.put(status, count);
        }

        long process = counts.getOrDefault(BookingStatus.PROCESS, 0L);
        long pnr = counts.getOrDefault(BookingStatus.PNR, 0L);
        long onHold = counts.getOrDefault(BookingStatus.ON_HOLD, 0L);
        long book = counts.getOrDefault(BookingStatus.BOOK, 0L);
        long confirmed = counts.getOrDefault(BookingStatus.CONFIRMED, 0L);
        long validationProcess = counts.getOrDefault(BookingStatus.VALIDATION_PROCESS, 0L);
        long validationPriceChanged = counts.getOrDefault(BookingStatus.VALIDATION_PRICE_CHANGED, 0L);
        long reprice = counts.getOrDefault(BookingStatus.REPRICE, 0L);

        long totalOpen = counts.values().stream().mapToLong(Long::longValue).sum();
        long needsAdminAction = BOOKING_NEEDS_ADMIN_ACTION_STATUSES.stream()
                .mapToLong(status -> counts.getOrDefault(status, 0L))
                .sum();

        return BookingStatusPendingStats.builder()
                .process(process)
                .pnr(pnr)
                .onHold(onHold)
                .book(book)
                .confirmed(confirmed)
                .validationProcess(validationProcess)
                .validationPriceChanged(validationPriceChanged)
                .reprice(reprice)
                .totalOpen(totalOpen)
                .needsAdminAction(needsAdminAction)
                .pendingApproval(pnr)
                .build();
    }

    private TicketActionPendingStats buildTicketActionPendingStats() {
        Map<TicketActionType, EnumMap<TicketActionStatus, Long>> grouped = new EnumMap<>(TicketActionType.class);
        for (TicketActionType type : TicketActionType.values()) {
            grouped.put(type, new EnumMap<>(TicketActionStatus.class));
        }

        for (Object[] row : ticketActionRequestRepository.countGroupedByTypeAndStatus(OPEN_TICKET_ACTION_STATUSES)) {
            TicketActionType type = (TicketActionType) row[0];
            TicketActionStatus status = (TicketActionStatus) row[1];
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            grouped.computeIfAbsent(type, k -> new EnumMap<>(TicketActionStatus.class))
                    .put(status, count);
        }

        TicketActionTypeBreakdown totals = aggregateTicketActionBreakdown(grouped);
        return TicketActionPendingStats.builder()
                .totals(totals)
                .cancel(breakdownForType(grouped, TicketActionType.CANCEL))
                .voidType(breakdownForType(grouped, TicketActionType.VOID))
                .refund(breakdownForType(grouped, TicketActionType.REFUND))
                .reissue(breakdownForType(grouped, TicketActionType.REISSUE))
                .build();
    }

    private TicketActionTypeBreakdown breakdownForType(
            Map<TicketActionType, EnumMap<TicketActionStatus, Long>> grouped,
            TicketActionType type) {
        EnumMap<TicketActionStatus, Long> counts = grouped.getOrDefault(type, new EnumMap<>(TicketActionStatus.class));
        return toBreakdown(counts);
    }

    private TicketActionTypeBreakdown aggregateTicketActionBreakdown(
            Map<TicketActionType, EnumMap<TicketActionStatus, Long>> grouped) {
        EnumMap<TicketActionStatus, Long> totals = new EnumMap<>(TicketActionStatus.class);
        for (EnumMap<TicketActionStatus, Long> byStatus : grouped.values()) {
            byStatus.forEach((status, count) ->
                    totals.merge(status, count, Long::sum));
        }
        return toBreakdown(totals);
    }

    private TicketActionTypeBreakdown toBreakdown(EnumMap<TicketActionStatus, Long> counts) {
        long submitted = counts.getOrDefault(TicketActionStatus.SUBMITTED, 0L);
        long quoted = counts.getOrDefault(TicketActionStatus.QUOTED, 0L);
        long userConfirmed = counts.getOrDefault(TicketActionStatus.USER_CONFIRMED, 0L);
        long adminProcessing = counts.getOrDefault(TicketActionStatus.ADMIN_PROCESSING, 0L);
        long totalOpen = submitted + quoted + userConfirmed + adminProcessing;
        long needsAdminAction = submitted + userConfirmed + adminProcessing;

        return TicketActionTypeBreakdown.builder()
                .submitted(submitted)
                .quoted(quoted)
                .userConfirmed(userConfirmed)
                .adminProcessing(adminProcessing)
                .totalOpen(totalOpen)
                .needsAdminAction(needsAdminAction)
                .build();
    }

    private static long safe(Long value) {
        return value != null ? value : 0L;
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
