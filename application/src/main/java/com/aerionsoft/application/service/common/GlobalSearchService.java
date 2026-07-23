package com.aerionsoft.application.service.common;

import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import com.aerionsoft.application.dto.admin.client.AgencyUserDto;
import com.aerionsoft.application.dto.booking.BookingResponse;
import com.aerionsoft.application.dto.search.GlobalSearchResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.booking.BookingClass;
import com.aerionsoft.application.enums.booking.SearchType;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.util.DepositBankMapper;
import com.aerionsoft.application.util.TimestampMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final BusinessRepository businessRepository;
    private final TimestampMapper timestampMapper;

    public GlobalSearchResponse search(String query, SearchType type) {
        if (query == null || query.trim().isEmpty()) {
            return GlobalSearchResponse.builder()
                    .bookings(new ArrayList<>())
                    .agents(new ArrayList<>())
                    .deposits(new ArrayList<>())
                    .searchQuery(query)
                    .totalResults(0)
                    .build();
        }

        String sanitizedQuery = query.trim();

        // Determine what to search based on type parameter
        List<BookingResponse> bookings = new ArrayList<>();
        List<AgencyUserDto> agents = new ArrayList<>();
        List<WalletDepositResponse> deposits = new ArrayList<>();

        // Default to ALL if type is null
        SearchType searchType = (type != null) ? type : SearchType.ALL;

        switch (searchType) {
            case ALL:
                // Search all types
                bookings = searchBookings(sanitizedQuery);
                agents = searchAgents(sanitizedQuery);
                deposits = searchDeposits(sanitizedQuery);
                break;
            case BOOKING:
                // Search only bookings
                bookings = searchBookings(sanitizedQuery);
                break;
            case AGENT:
                // Search only agents
                agents = searchAgents(sanitizedQuery);
                break;
            case DEPOSIT:
                // Search only deposits
                deposits = searchDeposits(sanitizedQuery);
                break;
        }

        int totalResults = bookings.size() + agents.size() + deposits.size();

        return GlobalSearchResponse.builder()
                .bookings(bookings)
                .agents(agents)
                .deposits(deposits)
                .searchQuery(sanitizedQuery)
                .totalResults(totalResults)
                .build();
    }

    private List<BookingResponse> searchBookings(String query) {
        // Search by PNR using LIKE query (partial match)
        List<Booking> bookings = bookingRepository.findByPnrContainingIgnoreCase(query);

        // Convert to BookingResponse
        return bookings.stream()
                .map(this::mapBookingToResponse)
                .collect(Collectors.toList());
    }

    private List<AgencyUserDto> searchAgents(String query) {
        // Search by name, email, phone, or code using LIKE query (partial match)
        List<User> users = userRepository.searchByNameEmailPhoneOrCode(query);

        // Convert to AgencyUserDto
        return users.stream()
                .map(this::mapUserToAgencyDto)
                .collect(Collectors.toList());
    }

    private List<WalletDepositResponse> searchDeposits(String query) {
        // Search by reference using LIKE query (partial match)
        List<WalletDeposit> deposits = walletDepositRepository.findByReferenceContainingIgnoreCase(query);

        // Convert to WalletDepositResponse
        return deposits.stream()
                .map(this::mapDepositToResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse mapBookingToResponse(Booking booking) {
        // Parse traveller IDs
        List<Long> travellerIds = new ArrayList<>();
        if (booking.getTravellerIds() != null && !booking.getTravellerIds().isEmpty()) {
            String[] ids = booking.getTravellerIds().split(",");
            for (String id : ids) {
                try {
                    travellerIds.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException e) {
                    // Skip invalid IDs
                }
            }
        }

        Double markupInUserCurrency = Double.parseDouble(booking.getMarkupAmount()) * Double.parseDouble(booking.getExchangeCurrencyRate());
        Double bookingPriceInUserCurrency = Double.parseDouble(booking.getBookingPrice()) * Double.parseDouble(booking.getExchangeCurrencyRate());
        Double originalPriceInUserCurrency = Double.parseDouble(booking.getOriginalPrice()) * Double.parseDouble(booking.getExchangeCurrencyRate());
        Double buyPriceInUserCurrency = booking.getBuyPrice() != null && !booking.getBuyPrice().isBlank()
                ? Double.parseDouble(booking.getBuyPrice()) * Double.parseDouble(booking.getExchangeCurrencyRate())
                : originalPriceInUserCurrency;
        Double profitLossInUserCurrency = booking.getProfitLoss() != null && !booking.getProfitLoss().isBlank()
                ? Double.parseDouble(booking.getProfitLoss()) * Double.parseDouble(booking.getExchangeCurrencyRate())
                : bookingPriceInUserCurrency - buyPriceInUserCurrency;

        return BookingResponse.builder()
                .id(booking.getId())
                .providerName(booking.getProviderName())
                .bookingClass(booking.getBookingClass() != null ?
                        BookingClass.fromValue(Integer.parseInt(booking.getBookingClass())) : null)
                .type(booking.getType())
                .bookingDate(booking.getBookingDate())
                .pnr(booking.getPnr())
                .ticketNo(booking.getTicketNo())
                .description(booking.getDescription())
                .airline(booking.getAirline())
                .status(booking.getStatus())
                .createdAt(timestampMapper.toRequestUserTime(
                        booking.getCreatedAt(),
                        booking.getCreatedTimeOffset() != null ? booking.getCreatedTimeOffset() : booking.getTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(
                        booking.getUpdatedAt(),
                        booking.getUpdatedTimeOffset() != null ? booking.getUpdatedTimeOffset() : booking.getTimeOffset()))
                .ticketingTime(timestampMapper.toRequestUserTime(
                        booking.getTicketingTime(),
                        booking.getTicketingTimeOffset() != null ? booking.getTicketingTimeOffset() : booking.getTimeOffset()))
                .createdBy(booking.getCreatedBy() != null ? booking.getCreatedBy().getId() : null)
                .createdByName(booking.getCreatedByName())
                .travellerIds(travellerIds)
                .channel(booking.getChannel())
                .exchangeCurrencyRate(booking.getExchangeCurrencyRate())
                .exchangeCurrency(booking.getExchangeCurrency())
                .TripType(booking.getTripType() != null ? booking.getTripType().toString() : null)
                .lastPaymentDate(booking.getLastPaymentDate())
                .bookingPrice(String.valueOf(bookingPriceInUserCurrency))
                .originalPrice(String.valueOf(originalPriceInUserCurrency))
                .buyPrice(String.valueOf(buyPriceInUserCurrency))
                .profitLoss(String.valueOf(profitLossInUserCurrency))
                .markupAmount(String.valueOf(markupInUserCurrency))
                .bookingReference(booking.getBookingReference())
                .timeOffset(booking.getTimeOffset())
                .bookedTimeOffset(booking.getBookedTimeOffset())
                .groupTicketType(booking.getGroupTicketType())
                .build();
    }

    private AgencyUserDto mapUserToAgencyDto(User user) {
        AgencyUserDto dto = new AgencyUserDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setBalance(user.getBalance());
        dto.setStatus(user.getIsActive() ? 1 : 0);
        dto.setCreatedAt(timestampMapper.createdAtString(user));
        dto.setAgency(user.isAgency());
        dto.setCode(user.getCode());
        dto.setAgentCode(user.getCode());

        if (user.getBusiness() == null && user.getParentUser() == null) {
            Optional<BusinessEntity> business = businessRepository.findFirstByMotherUser(user);
            dto.setBusinessId(
                    business.map(BusinessEntity::getId).orElse(null)
            );
            dto.setMotherUserId(
                    Optional.ofNullable(user.getParentUser())
                            .map(User::getId)
                            .orElse(null)
            );
        }else {
            dto.setBusinessId(
                    Optional.ofNullable(user.getBusiness())
                            .map(BusinessEntity::getId)
                            .orElse(null)
            );
            dto.setMotherUserId(
                    Optional.ofNullable(user.getParentUser())
                            .map(User::getId)
                            .orElse(null)
            );
        }

        return dto;
    }

    private WalletDepositResponse mapDepositToResponse(WalletDeposit deposit) {
        // Find user name for display
        String createdByName = "";
        if (deposit.getUserId() != null) {
            Optional<User> user = userRepository.findById(deposit.getUserId());
            createdByName = user.map(User::getFullName).orElse("");
        }

        String approvedByName = "";
        if (deposit.getApprovedBy() != null) {
            Optional<User> approver = userRepository.findById(deposit.getApprovedBy());
            approvedByName = approver.map(User::getFullName).orElse("");
        }

        return WalletDepositResponse.builder()
                .id(deposit.getId())
                .reference(deposit.getReference())
                .type(deposit.getType())
                .status(deposit.getStatus())
                .amount(deposit.getAmount())
                .exchangedAmount(deposit.getExchangedAmount())
                .exchangeRate(deposit.getExchangeRate())
                .remarks(deposit.getRemarks())
                .attachment(deposit.getAttachment())
                .chequeNo(deposit.getChequeNo())
                .chequeBank(deposit.getChequeBank())
                .chequeIssueDate(deposit.getChequeIssueDate())
                .depositBank(DepositBankMapper.resolve(deposit))
                .createdAt(timestampMapper.createdAt(deposit))
                .approvedAt(timestampMapper.toRequestUserTime(deposit.getApprovedAt(), deposit.getCreatedTimeOffset()))
                .createdBy(deposit.getUserId() != null ? deposit.getUserId().toString() : null)
                .approvedBy(approvedByName)
                .transactionId(deposit.getTransactionId())
                .createdByName(createdByName)
                .currency(deposit.getCurrency())
                .build();
    }
}

