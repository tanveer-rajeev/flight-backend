package com.aerionsoft.application.service.client;

import com.aerionsoft.application.context.UserTimezoneContext;
import com.aerionsoft.application.dto.client.user.DashboardStatsDto;
import com.aerionsoft.application.dto.client.user.TransactionDto;
import com.aerionsoft.application.dto.wallet.SourceEnrichment;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import com.aerionsoft.application.repository.tour.TourApplicationRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.booking.TravellerRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.repository.visa.VisaApplicationRepository;
import com.aerionsoft.application.service.wallet.TransactionEnrichmentService;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientDashBoardService {
    private final TravellerRepository travellerRepository;
    private final BookingRepository bookingRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final VisaApplicationRepository visaApplicationRepository;
    private final TransactionRepository transactionRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final TransactionEnrichmentService transactionEnrichmentService;
    private final TimestampMapper timestampMapper;

    public DashboardStatsDto getDashboardData(Long userId) {
        LocalDateTime now = UserDateTimeUtil.now();
        LocalDate today = now.toLocalDate();
        Instant rangeStart = now.minusMonths(1).atZone(UserTimezoneContext.getZoneId()).toInstant();
        Instant rangeEnd = now.atZone(UserTimezoneContext.getZoneId()).plusNanos(1).toInstant();
        Instant weekStart = now.minusWeeks(1).atZone(UserTimezoneContext.getZoneId()).toInstant();

        long totalTravellers = travellerRepository.countByCreatedBy(userId);
        long monthlyTickets = countUserBookingsInInstantRange(userId, rangeStart, rangeEnd);
        long lastWeekTickets = countUserBookingsInInstantRange(userId, weekStart, rangeEnd);
        long todayTickets = countUserBookingsOnUserDate(userId, today);

        long tourApplicationsLastMonth = tourApplicationRepository.countByCreatedByAndSubmittedAtBetween(
                String.valueOf(userId), now.minusMonths(1), now);
        long visaApplicationsLastMonth = visaApplicationRepository.countByCreatedByAndSubmittedAtBetween(
                String.valueOf(userId), now.minusMonths(1), now);

        long transactionsLastMonth = countUserTransactionsInInstantRange(userId, rangeStart, rangeEnd);
        long depositRequestsLastMonth = countUserDepositsInInstantRange(userId, rangeStart, rangeEnd);

        return DashboardStatsDto.builder()
                .totalTraveller(DashboardStatsDto.StatItem.builder()
                        .title("Total Traveller")
                        .count((int) totalTravellers)
                        .build())
                .montTicket(DashboardStatsDto.StatItem.builder()
                        .title("Monthly Ticket")
                        .count((int) monthlyTickets)
                        .build())
                .lastWeekTicket(DashboardStatsDto.StatItem.builder()
                        .title("Last Week Ticket")
                        .count((int) lastWeekTickets)
                        .build())
                .todayTicket(DashboardStatsDto.StatItem.builder()
                        .title("Today Ticket")
                        .count((int) todayTickets)
                        .build())
                .tureApplicationLastMonth(DashboardStatsDto.StatItem.builder()
                        .title("Tour Application (Last Month)")
                        .count((int) tourApplicationsLastMonth)
                        .build())
                .visaApplicationLastMonth(DashboardStatsDto.StatItem.builder()
                        .title("Visa Application (Last Month)")
                        .count((int) visaApplicationsLastMonth)
                        .build())
                .transactionLastMonth(DashboardStatsDto.StatItem.builder()
                        .title("Transactions (Last Month)")
                        .count((int) transactionsLastMonth)
                        .build())
                .depositRequestLastMonth(DashboardStatsDto.StatItem.builder()
                        .title("Deposit Requests (Last Month)")
                        .count((int) depositRequestsLastMonth)
                        .build())
                .build();
    }

    private long countUserBookingsOnUserDate(Long userId, LocalDate date) {
        Specification<Booking> spec = (root, query, cb) ->
                cb.equal(root.get("createdBy").get("id"), userId);
        Specification<Booking> dateSpec = OffsetAwareDateSpec.createdAtOnUserDate(
                date, "createdAt", "createdTimeOffset", "timeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return bookingRepository.count(spec);
    }

    private long countUserBookingsInInstantRange(Long userId, Instant startInclusive, Instant endExclusive) {
        Specification<Booking> spec = (root, query, cb) ->
                cb.equal(root.get("createdBy").get("id"), userId);
        Specification<Booking> dateSpec = OffsetAwareDateSpec.createdAtInInstantRange(
                startInclusive, endExclusive, "createdAt", "createdTimeOffset", "timeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return bookingRepository.count(spec);
    }

    private long countUserTransactionsInInstantRange(Long userId, Instant startInclusive, Instant endExclusive) {
        Specification<Transaction> spec = (root, query, cb) ->
                cb.equal(root.get("userId"), userId);
        Specification<Transaction> dateSpec = OffsetAwareDateSpec.createdAtInInstantRange(
                startInclusive, endExclusive, "createdAt", "createdTimeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return transactionRepository.count(spec);
    }

    private long countUserDepositsInInstantRange(Long userId, Instant startInclusive, Instant endExclusive) {
        Specification<WalletDeposit> spec = (root, query, cb) ->
                cb.equal(root.get("userId"), userId);
        Specification<WalletDeposit> dateSpec = OffsetAwareDateSpec.createdAtInInstantRange(
                startInclusive, endExclusive, "createdAt", "createdTimeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return walletDepositRepository.count(spec);
    }

    public Page<TransactionDto> getTransactions(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> transactions = transactionRepository.findByUserId(userId, pageRequest);
        var caches = transactionEnrichmentService.buildCaches(transactions.getContent());
        return transactions.map(t -> mapToDto(t, caches));
    }

    private TransactionDto mapToDto(Transaction t, java.util.Map<String, java.util.Map<Long, ?>> caches) {
        SourceEnrichment enrichment = transactionEnrichmentService.enrich(t, caches);
        TransactionDto.SourceSummary summary = null;
        if (enrichment.getLabel() != null || enrichment.getDetail() != null || enrichment.getStatus() != null) {
            summary = TransactionDto.SourceSummary.builder()
                    .label(enrichment.getLabel())
                    .detail(enrichment.getDetail())
                    .status(enrichment.getStatus())
                    .build();
        }
        return TransactionDto.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .convertedAmount(t.getConvertedAmount())
                .description(t.getDescription())
                .userId(t.getUserId())
                .createdBy(t.getCreatedBy())
                .createdAt(timestampMapper.createdAt(t))
                .updatedAt(timestampMapper.updatedAt(t, t.getCreatedTimeOffset()))
                .updatedBy(t.getUpdatedBy())
                .sourceType(t.getSourceType())
                .sourceId(t.getSourceId())
                .sourceSummary(summary)
                .build();
    }

}
