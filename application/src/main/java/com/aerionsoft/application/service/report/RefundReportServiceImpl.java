package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.RefundReportDTO;
import com.aerionsoft.application.dto.report.RefundTicketDTO;
import com.aerionsoft.application.dto.report.SalesReportTrendDTO;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.group.TravelInformation;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.repository.booking.TravelInformationRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import com.aerionsoft.application.util.TimestampMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundReportServiceImpl implements RefundReportService {

    private final BookingRepository bookingRepository;
    private final TravelInformationRepository travelInformationRepository;
    private final TimestampMapper timestampMapper;

    @Override
    @Transactional(readOnly = true)
    public RefundReportDTO getRefundReport(Long userId,
                                           Long agencyId,
                                           String airlineCode,
                                           String currency,
                                           LocalDate from,
                                           LocalDate to,
                                           int page,
                                           int size) {

        Specification<Booking> spec = buildSpec(userId, agencyId, airlineCode, currency, from, to);
        List<Booking> allMatched = bookingRepository.findAll(spec).stream()
                .filter(b -> timestampMapper.isBookingInUserDateRange(b, from, to))
                .toList();

        // Aggregate totals and trend
        double totalRefundedAmount = 0.0;
        Map<String, SalesReportTrendDTO> trendMap = new TreeMap<>();

        for (Booking b : allMatched) {
            double price = parseDouble(b.getBookingPrice());
            totalRefundedAmount += price;

            String createdOffset = b.getCreatedTimeOffset() != null ? b.getCreatedTimeOffset() : b.getTimeOffset();
            String updatedOffset = b.getUpdatedTimeOffset() != null ? b.getUpdatedTimeOffset() : b.getTimeOffset();
            LocalDateTime trendTime = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
            String trendOffset = b.getUpdatedAt() != null ? updatedOffset : createdOffset;
            LocalDateTime convertedTrendTime = timestampMapper.toRequestUserTime(trendTime, trendOffset);
            String dateStr = convertedTrendTime != null ? convertedTrendTime.toLocalDate().toString() : "UNKNOWN";

            trendMap.putIfAbsent(dateStr, new SalesReportTrendDTO(dateStr, 0L, 0.0));
            SalesReportTrendDTO trend = trendMap.get(dateStr);
            trend.setTotalTickets(trend.getTotalTickets() + 1);
            trend.setRevenue(trend.getRevenue() + price);
        }

        // Sort DESC by refundedAt (updatedAt) before paging
        allMatched.sort(Comparator.comparing(
                b -> b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // Page
        int start = page * size;
        int end = Math.min(start + size, allMatched.size());
        List<Booking> paged = start < allMatched.size() ? allMatched.subList(start, end) : Collections.emptyList();

        List<RefundTicketDTO> dtos = paged.stream().map(b -> {
            TravelInformation ti = travelInformationRepository.findByBookingId(b.getId());
            String route = null;
            String airCode = b.getAirline();
            if (ti != null) {
                if (ti.getOrigin() != null && ti.getDestination() != null) {
                    route = ti.getOrigin() + " - " + ti.getDestination();
                }
                if (StringUtils.hasText(ti.getAirlineCode())) {
                    airCode = ti.getAirlineCode();
                }
            }

            User createdBy = b.getCreatedBy();
            String agencyName = resolveAgencyName(createdBy);
            String userCurrency = createdBy != null && StringUtils.hasText(createdBy.getCurrency())
                    ? createdBy.getCurrency().trim().toUpperCase()
                    : "USD";
            String createdOffset = b.getCreatedTimeOffset() != null ? b.getCreatedTimeOffset() : b.getTimeOffset();
            String updatedOffset = b.getUpdatedTimeOffset() != null ? b.getUpdatedTimeOffset() : b.getTimeOffset();

            return RefundTicketDTO.builder()
                    .bookingId(b.getId())
                    .pnr(b.getPnr())
                    .ticketNo(b.getTicketNo())
                    .bookingReference(b.getBookingReference())
                    .airline(b.getAirline())
                    .airlineCode(airCode)
                    .agencyName(agencyName)
                    .customerName(b.getCustomer())
                    .bookingPrice(parseDouble(b.getBookingPrice()))
                    .refundedAmount(parseDouble(b.getBookingPrice()))
                    .currency(userCurrency)
                    .bookingDate(timestampMapper.toRequestUserTime(b.getCreatedAt(), createdOffset))
                    .refundedAt(timestampMapper.toRequestUserTime(b.getUpdatedAt(), updatedOffset))
                    .status(b.getStatus() != null ? b.getStatus().name() : null)
                    .route(route)
                    .build();
        }).collect(Collectors.toList());

        Page<RefundTicketDTO> pageData = new PageImpl<>(dtos, PageRequest.of(page, size), allMatched.size());

        return RefundReportDTO.builder()
                .totalRefunds(allMatched.size())
                .totalRefundedAmount(totalRefundedAmount)
                .refundTrend(new ArrayList<>(trendMap.values()))
                .records(pageData)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Specification<Booking> buildSpec(Long userId, Long agencyId,
                                              String airlineCode, String currency,
                                              LocalDate from, LocalDate to) {
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), BookingStatus.REFUND));

            if (userId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), userId));
            }
            if (agencyId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("business").get("id"), agencyId));
            }
            if (airlineCode != null && !airlineCode.isBlank()) {
                predicates.add(cb.equal(root.get("airline"), airlineCode));
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
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return 0.0; }
    }

    private String resolveAgencyName(User createdBy) {
        if (createdBy == null) return null;
        User anchor = createdBy.getParentUser() != null ? createdBy.getParentUser() : createdBy;
        if (anchor.getBusiness() != null && StringUtils.hasText(anchor.getBusiness().getCompanyName())) {
            return anchor.getBusiness().getCompanyName().trim();
        }
        return StringUtils.hasText(anchor.getFullName()) ? anchor.getFullName().trim() : null;
    }
}

