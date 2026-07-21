package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.DailyReportResponseDTO;
import com.aerionsoft.application.dto.DepositSummaryDTO;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.util.UserDateTimeUtil;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DailyReportService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final BusinessRepository businessRepository;

    public DailyReportService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            WalletDepositRepository walletDepositRepository,
            BusinessRepository businessRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.walletDepositRepository = walletDepositRepository;
        this.businessRepository = businessRepository;
    }

    public DailyReportResponseDTO generateDailyReport() {
        DailyReportResponseDTO dailyReportResponseDTO = new DailyReportResponseDTO();
        LocalDate today = UserDateTimeUtil.now().toLocalDate();

        Specification<Booking> todaySpec = OffsetAwareDateSpec.createdAtOnUserDate(
                today, "createdAt", "createdTimeOffset");
        List<Booking> todayBookings = todaySpec != null
                ? bookingRepository.findAll(todaySpec)
                : List.of();

        Map<BookingStatus, Long> countsByStatus = new EnumMap<>(BookingStatus.class);
        for (Booking booking : todayBookings) {
            if (booking.getStatus() != null) {
                countsByStatus.merge(booking.getStatus(), 1L, Long::sum);
            }
        }

        for (Map.Entry<BookingStatus, Long> entry : countsByStatus.entrySet()) {
            int count = entry.getValue().intValue();
            switch (entry.getKey()) {
                case PROCESS -> dailyReportResponseDTO.setTotalBookingProcessed(count);
                case PNR -> dailyReportResponseDTO.setTotalBookingPnr(count);
                case CONFIRMED -> dailyReportResponseDTO.setTotalBookingConfirmed(count);
                case CANCELLED -> dailyReportResponseDTO.setTotalBookingCanceled(count);
                case TICKETED -> dailyReportResponseDTO.setTotalBookingTicketed(count);
                case ON_HOLD -> dailyReportResponseDTO.setTotalBookingOnHold(count);
                case VOID -> dailyReportResponseDTO.setTotalBookingVoided(count);
                case TICKET_ISSUED -> dailyReportResponseDTO.setTotalBookingTicketIssued(count);
                default -> { }
            }
        }

        DepositSummaryDTO summary = buildDepositSummaryForUserDate(today);

        dailyReportResponseDTO.setTotalPendingDepositBdt(summary.getPendingBdt());
        dailyReportResponseDTO.setTotalPendingDepositInr(summary.getPendingInr());
        dailyReportResponseDTO.setTotalPendingDepositUsd(summary.getPendingUsd());
        dailyReportResponseDTO.setTotalPendingDepositPkr(summary.getPendingPkr());
        dailyReportResponseDTO.setTotalPendingDepositSar(summary.getPendingSar());
        dailyReportResponseDTO.setTotalPendingDepositQar(summary.getPendingQar());

        dailyReportResponseDTO.setTotalApprovedDepositBdt(summary.getApprovedBdt());
        dailyReportResponseDTO.setTotalApprovedDepositInr(summary.getApprovedInr());
        dailyReportResponseDTO.setTotalApprovedDepositUsd(summary.getApprovedUsd());
        dailyReportResponseDTO.setTotalApprovedDepositPkr(summary.getApprovedPkr());
        dailyReportResponseDTO.setTotalApprovedDepositSar(summary.getApprovedSar());
        dailyReportResponseDTO.setTotalApprovedDepositQar(summary.getApprovedQar());

        Specification<User> userDateSpec = OffsetAwareDateSpec.createdAtOnUserDate(
                today, "createdAt", "createdTimeOffset");
        long totalUser = userDateSpec != null ? userRepository.count(userDateSpec) : 0L;

        Specification<BusinessEntity> businessDateSpec = OffsetAwareDateSpec.createdAtOnUserDate(
                today, "createdAt", "createdTimeOffset");
        long totalBusiness = businessDateSpec != null ? businessRepository.count(businessDateSpec) : 0L;

        dailyReportResponseDTO.setTotalUserCreated(Math.toIntExact(totalUser));
        dailyReportResponseDTO.setTotalAgencyCreated(Math.toIntExact(totalBusiness));

        return dailyReportResponseDTO;
    }

    private DepositSummaryDTO buildDepositSummaryForUserDate(LocalDate date) {
        Specification<WalletDeposit> dateSpec = OffsetAwareDateSpec.createdAtOnUserDate(
                date, "createdAt", "createdTimeOffset");
        List<WalletDeposit> deposits = dateSpec != null
                ? walletDepositRepository.findAll(dateSpec)
                : List.of();

        double pendingBdt = 0, pendingInr = 0, pendingUsd = 0, pendingPkr = 0, pendingSar = 0, pendingQar = 0;
        double approvedBdt = 0, approvedInr = 0, approvedUsd = 0, approvedPkr = 0, approvedSar = 0, approvedQar = 0;

        for (WalletDeposit deposit : deposits) {
            if (deposit.getCurrency() == null || deposit.getAmount() == null) {
                continue;
            }
            double amount = deposit.getAmount();
            boolean pending = deposit.getStatus() == DepositStatus.PENDING;
            boolean approved = deposit.getStatus() == DepositStatus.APPROVED;
            switch (deposit.getCurrency()) {
                case BDT -> {
                    if (pending) pendingBdt += amount;
                    if (approved) approvedBdt += amount;
                }
                case INR -> {
                    if (pending) pendingInr += amount;
                    if (approved) approvedInr += amount;
                }
                case USD -> {
                    if (pending) pendingUsd += amount;
                    if (approved) approvedUsd += amount;
                }
                case PKR -> {
                    if (pending) pendingPkr += amount;
                    if (approved) approvedPkr += amount;
                }
                case SAR -> {
                    if (pending) pendingSar += amount;
                    if (approved) approvedSar += amount;
                }
                case QAR -> {
                    if (pending) pendingQar += amount;
                    if (approved) approvedQar += amount;
                }
                default -> { }
            }
        }

        return new DepositSummaryDTO(
                pendingBdt, pendingInr, pendingUsd, pendingPkr, pendingSar, pendingQar,
                approvedBdt, approvedInr, approvedUsd, approvedPkr, approvedSar, approvedQar
        );
    }
}
