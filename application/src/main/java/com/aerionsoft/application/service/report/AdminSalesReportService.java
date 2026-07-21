package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.AdminSalesReportDTO;
import com.aerionsoft.application.dto.report.ConfirmedTicketDTO;
import com.aerionsoft.application.dto.report.SalesReportTrendDTO;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.group.TravelInformation;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.repository.booking.TravelInformationRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminSalesReportService {
    private final BookingRepository bookingRepository;
    private final TravelInformationRepository travelInformationRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final CurrencyService currencyService;
    private final TimestampMapper timestampMapper;

    public AdminSalesReportService(BookingRepository bookingRepository,
                                   TravelInformationRepository travelInformationRepository,
                                   UserRepository userRepository,
                                   BusinessRepository businessRepository,
                                   CurrencyService currencyService,
                                   TimestampMapper timestampMapper) {
        this.bookingRepository = bookingRepository;
        this.travelInformationRepository = travelInformationRepository;
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.currencyService = currencyService;
        this.timestampMapper = timestampMapper;
    }

    @Transactional(readOnly = true)
    public AdminSalesReportDTO getSalesReport(Long userId, Long agencyId, String airlineCode,
                                              String currency, LocalDate from, LocalDate to, int page, int size) {
        LocalDate effectiveFrom = from;
        LocalDate effectiveTo = to;
        if (from == null && to == null) {
            LocalDate today = UserDateTimeUtil.now().toLocalDate();
            effectiveFrom = today;
            effectiveTo = today;
        }
        final LocalDate filterFrom = effectiveFrom;
        final LocalDate filterTo = effectiveTo;

        Specification<Booking> spec =
                buildSpecification(userId, agencyId, airlineCode, currency, effectiveFrom, effectiveTo);

        List<Booking> allMatched = bookingRepository.findAll(spec).stream()
                .filter(b -> timestampMapper.isBookingInUserDateRange(b, filterFrom, filterTo))
                .toList();

        long totalTickets = allMatched.size();
        double totalRevenue = 0.0;  // profit/loss = sellPrice - buyPrice
        double totalTax = 0.0;

        Map<String, SalesReportTrendDTO> trendMap = new TreeMap<>(); // sorted by date string

        for (Booking b : allMatched) {
            double profitLoss = resolveProfitLossUsd(b);
            double tax = b.getTaxAmount() != null ? b.getTaxAmount() : 0.0;

            totalRevenue += profitLoss;
            totalTax += tax;

            String createdOffset = timestampMapper.bookingStoredOffset(b);
            LocalDateTime convertedCreatedAt = timestampMapper.toRequestUserTime(b.getCreatedAt(), createdOffset);
            String dateStr = convertedCreatedAt != null ? convertedCreatedAt.toLocalDate().toString() : "UNKNOWN";

            trendMap.putIfAbsent(dateStr, new SalesReportTrendDTO(dateStr, 0L, 0.0));
            SalesReportTrendDTO trend = trendMap.get(dateStr);
            trend.setTotalTickets(trend.getTotalTickets() + 1);
            trend.setRevenue(trend.getRevenue() + profitLoss);
        }

        double netRevenue = totalRevenue;
        double revenueAvg = totalTickets > 0 ? totalRevenue / totalTickets : 0.0;

        List<SalesReportTrendDTO> trendList = new ArrayList<>(trendMap.values());

        // Sort by user-local created time desc before paging
        allMatched = new ArrayList<>(allMatched);
        allMatched.sort(Comparator.comparing(
                timestampMapper::bookingCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int start = page * size;
        int end = Math.min(start + size, allMatched.size());
        List<Booking> pagedBookings = start < allMatched.size() ? allMatched.subList(start, end) : Collections.emptyList();

        Set<Long> creatorUserIds = pagedBookings.stream()
                .map(Booking::getCreatedBy)
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toSet());
        Map<Long, String> agencyNameByCreatorUserId = buildAgencyNameByCreatorUserId(creatorUserIds);

        Set<Long> pagedBookingIds = pagedBookings.stream()
                .map(Booking::getId)
                .collect(Collectors.toSet());
        Map<Long, TravelInformation> travelByBookingId = pagedBookingIds.isEmpty()
                ? Map.of()
                : travelInformationRepository.findByBookingIdIn(pagedBookingIds).stream()
                .collect(Collectors.toMap(TravelInformation::getBookingId, t -> t, (left, right) -> left));

        List<ConfirmedTicketDTO> pagedDtos = pagedBookings.stream().map(b -> {
            TravelInformation t = travelByBookingId.get(b.getId());
            String tAirlineCode = t != null ? t.getAirlineCode() : b.getAirline();
            User createdBy = b.getCreatedBy();
            String userCurrency = userCurrencyFromBooker(createdBy);
            double rate = b.getExchangeCurrencyRate() != null ? Double.parseDouble(b.getExchangeCurrencyRate()) : exchangeRateUsdToUserCurrency(b, userCurrency);

            double originalUsd = parseDouble(b.getOriginalPrice());
            double bookingUsd = parseDouble(b.getBookingPrice());
            double buyUsd = resolveBuyPriceUsd(b);
            double profitLossUsd = resolveProfitLossUsd(b);

            double originalInUser = roundCurrency(originalUsd * rate);
            double bookingInUser = roundCurrency(bookingUsd * rate);
            double buyInUser = roundCurrency(buyUsd * rate);
            double profitLossInUser = roundCurrency(profitLossUsd * rate);
            double taxUsd = b.getTaxAmount() != null ? b.getTaxAmount() : 0.0;
            double taxInUser = roundCurrency(taxUsd * rate);

            return ConfirmedTicketDTO.builder()
                .bookingId(b.getId())
                .pnr(b.getPnr())
                .ticketNo(b.getTicketNo())
                .airline(b.getAirline())
                .airlineCode(tAirlineCode)
                .agencyName(createdBy != null ? agencyNameByCreatorUserId.get(createdBy.getId()) : null)
                .customerName(b.getCustomer())
                .originalPrice(originalInUser)
                .buyPrice(buyInUser)
                .bookingPrice(bookingInUser)
                .markupAmount(profitLossInUser)
                .profitLoss(profitLossInUser)
                .totalFare(bookingInUser)
                .tax(taxInUser)
                .currency(userCurrency)
                .bookingDate(timestampMapper.bookingCreatedAt(b))
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .build();
        }).collect(Collectors.toList());

        Page<ConfirmedTicketDTO> pageData = new PageImpl<>(pagedDtos, PageRequest.of(page, size), allMatched.size());

        return AdminSalesReportDTO.builder()
                .totalTickets(totalTickets)
                .totalRevenue(totalRevenue)
                .totalTax(totalTax)
                .netRevenue(netRevenue)
                .revenueAvg(revenueAvg)
                .salesTrend(trendList)
                .confirmedTickets(pageData)
                .build();
    }

    private Specification<Booking> buildSpecification(Long userId, Long agencyId, String airlineCode,
                                                      String currency, LocalDate from, LocalDate to) {
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(root.get("status").in(
                    BookingStatus.CONFIRMED,
                    BookingStatus.TICKETED,
                    BookingStatus.TICKET_ISSUED,
                    BookingStatus.COMPLETED
            ));

            if (userId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), userId));
            }

            if (agencyId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("business").get("id"), agencyId));
            }

            if (airlineCode != null && !airlineCode.isBlank()) {
                Subquery<Integer> subquery = query.subquery(Integer.class);
                Root<TravelInformation> travelRoot = subquery.from(TravelInformation.class);
                subquery.select(cb.literal(1));
                subquery.where(
                        cb.equal(travelRoot.get("bookingId"), root.get("id")),
                        cb.equal(travelRoot.get("airlineCode"), airlineCode)
                );
                predicates.add(cb.exists(subquery));
            }

            if (currency != null && !currency.isBlank()) {
                predicates.add(cb.equal(root.get("exchangeCurrency"), currency));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Specification<Booking> dateSpec = OffsetAwareDateSpec.createdAtInUserRange(
                from, to, "createdAt", "createdTimeOffset", "timeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return spec;
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** Buy price in stored USD; falls back to original price for legacy/manual/import rows. */
    private double resolveBuyPriceUsd(Booking booking) {
        double buyPrice = parseDouble(booking.getBuyPrice());
        if (buyPrice <= 0) {
            buyPrice = parseDouble(booking.getOriginalPrice());
        }
        return buyPrice;
    }

    /** Profit/loss in stored USD; falls back to sell − buy when column is empty. */
    private double resolveProfitLossUsd(Booking booking) {
        if (booking.getProfitLoss() != null && !booking.getProfitLoss().isBlank()) {
            return parseDouble(booking.getProfitLoss());
        }
        return parseDouble(booking.getBookingPrice()) - resolveBuyPriceUsd(booking);
    }

    /** Same convention as BookingService wallet deduction: creator's wallet currency. */
    private String userCurrencyFromBooker(User createdBy) {
        if (createdBy == null || !StringUtils.hasText(createdBy.getCurrency())) {
            return "USD";
        }
        return createdBy.getCurrency().trim().toUpperCase();
    }

    private double exchangeRateUsdToUserCurrency(Booking booking, String userCurrency) {
        if ("USD".equalsIgnoreCase(userCurrency)) {
            return 1.0;
        }
        String provider =
                booking.getProviderName() != null ? booking.getProviderName().name() : "DEFAULT";
        try {
            return currencyService.getExchangeRateBasedOnUsd(userCurrency, provider, booking.getChannel());
        } catch (Exception e) {
            return 1.0;
        }
    }

    private static double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Resolves agency company names in bulk via businesses.mother_user_id
     * (sub-account bookings use the parent user's business).
     */
    private Map<Long, String> buildAgencyNameByCreatorUserId(Collection<Long> creatorUserIds) {
        if (creatorUserIds == null || creatorUserIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, User> usersById = userRepository.findByIdInWithParent(creatorUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));

        Set<Long> motherUserIds = new HashSet<>();
        for (Long userId : creatorUserIds) {
            User user = usersById.get(userId);
            if (user == null) {
                motherUserIds.add(userId);
                continue;
            }
            if (user.getParentUser() != null) {
                motherUserIds.add(user.getParentUser().getId());
            } else {
                motherUserIds.add(user.getId());
            }
        }

        Map<Long, String> agencyNameByMotherUserId = motherUserIds.isEmpty()
                ? Map.of()
                : businessRepository.findCompanyNamesByMotherUserIds(motherUserIds).stream()
                .filter(row -> row[1] != null && StringUtils.hasText((String) row[1]))
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((String) row[1]).trim(),
                        (left, right) -> left
                ));

        Map<Long, String> agencyNameByCreatorUserId = new HashMap<>();
        for (Long userId : creatorUserIds) {
            User user = usersById.get(userId);
            Long motherUserId = user != null && user.getParentUser() != null
                    ? user.getParentUser().getId()
                    : userId;
            String agencyName = agencyNameByMotherUserId.get(motherUserId);
            if (agencyName != null) {
                agencyNameByCreatorUserId.put(userId, agencyName);
            }
        }
        return agencyNameByCreatorUserId;
    }
}
