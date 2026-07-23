package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.dto.ticketaction.*;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.service.common.CurrencyService;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.Booking.TicketActionRequest;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.TicketActionStatus;
import com.aerionsoft.application.enums.booking.TicketActionType;
import com.aerionsoft.application.repository.booking.TicketActionRequestRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.service.audit.ActivityTicketActionAuditSupport;
import com.aerionsoft.application.service.notification.NotificationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TicketActionRequestService {

    private final TicketActionRequestRepository ticketActionRequestRepository;
    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;
    private final NotificationHelper notificationHelper;
    private final AdminUserRepository adminUserRepository;
    private final TimestampMapper timestampMapper;
    private final ActivityTicketActionAuditSupport activityTicketActionAuditSupport;

    public TicketActionRequestService(TicketActionRequestRepository ticketActionRequestRepository,
                                      BookingService bookingService,
                                      UserRepository userRepository,
                                      CurrencyService currencyService,
                                      NotificationHelper notificationHelper,
                                      AdminUserRepository adminUserRepository,
                                      TimestampMapper timestampMapper,
                                      ActivityTicketActionAuditSupport activityTicketActionAuditSupport) {
        this.ticketActionRequestRepository = ticketActionRequestRepository;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.currencyService = currencyService;
        this.notificationHelper = notificationHelper;
        this.adminUserRepository = adminUserRepository;
        this.timestampMapper = timestampMapper;
        this.activityTicketActionAuditSupport = activityTicketActionAuditSupport;
    }

    private static final List<TicketActionStatus> OPEN_STATUSES = List.of(
            TicketActionStatus.SUBMITTED,
            TicketActionStatus.QUOTED,
            TicketActionStatus.USER_CONFIRMED,
            TicketActionStatus.ADMIN_PROCESSING
    );


    /**
     * Notify all admin users about a ticket action request event
     */
    private void notifyAdminsAboutTicketAction(TicketActionRequest ticketAction, String eventType) {
        try {
            // Get all admins with ADMIN role
            List<AdminUser> admins = adminUserRepository.findAdminsByRoleSlug("admin");

            if (admins == null || admins.isEmpty()) {
                log.warn("No admin users found to notify about ticket action request");
                return;
            }

            String userName = ticketAction.getRequester() != null ?
                    (ticketAction.getRequester().getFullName() != null ?
                            ticketAction.getRequester().getFullName() :
                            ticketAction.getRequester().getEmail()) :
                    "Unknown User";
            String pnr = ticketAction.getBooking().getPnr() != null ? ticketAction.getBooking().getPnr() : "N/A";
            String ticketNo = ticketAction.getBooking().getTicketNo() != null ? ticketAction.getBooking().getTicketNo() : "N/A";
            String actionType = ticketAction.getType().name();

            String title, message;

            if ("SUBMITTED".equals(eventType)) {
                title = "New Ticket Action Request";
                message = String.format("User %s has submitted a %s request for PNR: %s, Ticket: %s",
                        userName, actionType, pnr, ticketNo);
            } else if ("USER_CONFIRMED".equals(eventType)) {
                title = "Ticket Action Quote Confirmed";
                message = String.format("User %s has confirmed the %s quote for PNR: %s. Ready for processing.",
                        userName, actionType, pnr);
            } else {
                return; // Only notify for SUBMITTED and USER_CONFIRMED events
            }

            for (AdminUser admin : admins) {
                if (admin != null && admin.getId() != null) {
                    try {
                        notificationHelper.sendCustomNotification(
                                admin.getId(),
                                NotificationType.TICKET_ACTION_SUBMITTED,
                                NotificationPriority.HIGH,
                                title,
                                message,
                                "/admin/ticket-actions/" + ticketAction.getId(),
                                "View Request"
                        );
                        log.info("Notified admin {} about ticket action request {}", admin.getId(), ticketAction.getId());
                    } catch (Exception e) {
                        log.error("Failed to notify admin {} about ticket action request {}", admin.getId(), ticketAction.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify admins about ticket action request {}", ticketAction.getId(), e);
        }
    }

    @Transactional
    public TicketActionRequestResponse create(Long bookingId, TicketActionRequestCreateRequest request, Long currentUserId, boolean isAdmin) {
        Booking booking = bookingService.getBookingById(bookingId);

        // Access control: admin OR owner OR child of owner
        enforceBookingAccess(booking, currentUserId, isAdmin);

        // Auto-expire quoted requests if needed before applying duplicate/open checks
        autoRejectExpiredQuotes(bookingId);

        // prevent duplicates for same booking+type while open
        if (ticketActionRequestRepository.existsByBookingIdAndTypeAndStatusIn(bookingId, request.getType(), OPEN_STATUSES)) {
            throw ServiceExceptions.duplicate("An open " + request.getType() + " request already exists for this booking");
        }

        // Block incoming requests if there is ANY quoted request still pending acceptance (business rule)
        if (ticketActionRequestRepository.existsByBookingIdAndStatus(bookingId, TicketActionStatus.QUOTED)) {
            throw ServiceExceptions.accessDenied("A quoted ticket action is pending customer confirmation. Please confirm or wait for expiry.");
        }

        User requester = userRepository.findById(currentUserId).orElseThrow(() -> new ResourceNotFoundException("User"));

        if (request.getType() == TicketActionType.REISSUE && request.getReissueDate() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "reissueDate is required when submitting a REISSUE request");
        }

        TicketActionRequest entity = TicketActionRequest.builder()
                .booking(booking)
                .requester(requester)
                .type(request.getType())
                .status(TicketActionStatus.SUBMITTED)
                .reason(request.getReason())
                .reissueDate(request.getType() == TicketActionType.REISSUE ? request.getReissueDate() : null)
                .createdTimeOffset(UserDateTimeUtil.currentOffset())
                .build();

        TicketActionRequest saved = ticketActionRequestRepository.save(entity);

        activityTicketActionAuditSupport.logSubmitted(saved);

        // Notify admins about new request
        notifyAdminsAboutTicketAction(saved, "SUBMITTED");

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TicketActionRequestResponse> listForBooking(Long bookingId, int page, int size, Long currentUserId, boolean isAdmin) {
        Booking booking = bookingService.getBookingById(bookingId);
        enforceBookingAccess(booking, currentUserId, isAdmin);

        // best-effort auto-expire on read
        autoRejectExpiredQuotes(bookingId);

        return ticketActionRequestRepository.findByBookingId(bookingId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TicketActionRequestResponse getForBooking(Long bookingId, Long requestId, Long currentUserId, boolean isAdmin) {
        // best-effort auto-expire on read
        autoRejectExpiredQuotes(bookingId);

        TicketActionRequest tar = ticketActionRequestRepository.findByIdAndBookingId(requestId, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket action request"));

        enforceBookingAccess(tar.getBooking(), currentUserId, isAdmin);
        return toResponse(tar);
    }

    @Transactional
    public TicketActionRequestResponse userConfirm(Long bookingId, Long requestId, Long currentUserId, boolean isAdmin) {
        // auto-expire before confirm
        autoRejectExpiredQuotes(bookingId);

        TicketActionRequest tar = ticketActionRequestRepository.findByIdAndBookingId(requestId, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket action request"));

        enforceBookingAccess(tar.getBooking(), currentUserId, isAdmin);

        if (tar.getStatus() != TicketActionStatus.QUOTED) {
            throw ServiceExceptions.accessDenied("Only QUOTED requests can be confirmed");
        }

        tar.setStatus(TicketActionStatus.USER_CONFIRMED);
        tar.setUserConfirmedAt(UserDateTimeUtil.now());
        TicketActionRequest saved = ticketActionRequestRepository.save(tar);

        activityTicketActionAuditSupport.logUserConfirmed(saved);

        // Notify admins that user confirmed
        notifyAdminsAboutTicketAction(saved, "USER_CONFIRMED");

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TicketActionRequestResponse> adminList(String status, String type, int page, int size) {
        TicketActionStatus st = null;
        TicketActionType tp = null;

        if (status != null && !status.isBlank()) {
            st = TicketActionStatus.valueOf(status.toUpperCase());
        }
        if (type != null && !type.isBlank()) {
            tp = TicketActionType.valueOf(type.toUpperCase());
        }

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        if (st != null && tp != null) {
            return ticketActionRequestRepository.findByStatusAndType(st, tp, pageable).map(this::toResponse);
        }
        if (st != null) {
            return ticketActionRequestRepository.findByStatus(st, pageable).map(this::toResponse);
        }
        if (tp != null) {
            return ticketActionRequestRepository.findByType(tp, pageable).map(this::toResponse);
        }

        return ticketActionRequestRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public TicketActionRequestResponse adminQuote(Long requestId, AdminTicketActionQuoteRequest request, Long adminUserId) {
        TicketActionRequest tar = ticketActionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket action request"));

        if (tar.getStatus() != TicketActionStatus.SUBMITTED) {
            throw ServiceExceptions.accessDenied("Only SUBMITTED requests can be quoted");
        }

        tar.setQuoteAirlineCost(request.getAirlineCost());
        tar.setQuoteServiceCharge(request.getServiceCharge());
        tar.setQuoteTotalAmount(request.getTotalAmount());
        tar.setQuoteCurrency(request.getCurrency());
        tar.setQuoteDetails(request.getDetails());
        tar.setAdminNote(request.getAdminNote());

        tar.setAcceptDeadline(request.getAcceptDeadline());
        tar.setRefundTimeline(request.getRefundTimeline());

        tar.setQuotedAt(UserDateTimeUtil.now());
        tar.setQuotedByAdminId(adminUserId);
        tar.setStatus(TicketActionStatus.QUOTED);

        // Exchange rate and user currency
        Booking booking = tar.getBooking();
        String bookingCurrency = booking.getExchangeCurrency();
//        String exchangeRate = currencyService.getExchangeRate("USD", bookingCurrency, "OTHERS");

        tar.setQuoteUserCurrency(booking.getExchangeCurrency());
//        tar.setQuoteExchangeRate(new BigDecimal(exchangeRate));
        tar.setQuoteExchangeRate(BigDecimal.valueOf(0.0));

        TicketActionRequest saved = ticketActionRequestRepository.save(tar);

        activityTicketActionAuditSupport.logQuoted(saved, adminUserId);

        // Notify user that quote is ready (send email if configured later; for now keep in-app only)
        if (saved.getRequester() != null) {
            String email = saved.getRequester().getEmail();
            notificationHelper.sendTicketActionQuotedToUser(
                    saved.getRequester().getId(),
                    email,
                    saved.getBooking().getPnr(),
                    saved.getBooking().getId(),
                    saved.getType().name(),
                    saved.getQuoteTotalAmount() != null ? saved.getQuoteTotalAmount().toPlainString() : "0",
                    saved.getQuoteCurrency() != null ? saved.getQuoteCurrency() : (saved.getQuoteUserCurrency() != null ? saved.getQuoteUserCurrency() : ""),
                    saved.getId(),
                    email != null && !email.isBlank() // sendEmail
            );
        }

        return toResponse(saved);
    }

    @Transactional
    public TicketActionRequestResponse adminReject(Long requestId, AdminTicketActionRejectRequest request, Long adminUserId) {
        TicketActionRequest tar = ticketActionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket action request"));

        if (tar.getStatus() == TicketActionStatus.COMPLETED || tar.getStatus() == TicketActionStatus.REJECTED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Cannot reject a terminal request");
        }

        tar.setAdminNote(request.getAdminNote());
        tar.setQuotedByAdminId(adminUserId);
        tar.setStatus(TicketActionStatus.REJECTED);

        TicketActionRequest saved = ticketActionRequestRepository.save(tar);

        activityTicketActionAuditSupport.logRejected(saved, adminUserId, false);

        // Notify user about rejection
        if (saved.getRequester() != null) {
            String email = saved.getRequester().getEmail();
            notificationHelper.sendTicketActionRejectedToUser(
                    saved.getRequester().getId(),
                    email,
                    saved.getBooking().getPnr(),
                    saved.getBooking().getTicketNo(),
                    saved.getType().name(),
                    saved.getAdminNote(),
                    saved.getId(),
                    email != null && !email.isBlank()
            );
        }

        return toResponse(saved);
    }

    @Transactional
    public TicketActionRequestResponse adminStartProcessing(Long requestId, Long adminUserId) {
        TicketActionRequest tar = ticketActionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket action request"));

        if (tar.getStatus() != TicketActionStatus.USER_CONFIRMED) {
            throw ServiceExceptions.accessDenied("Only USER_CONFIRMED requests can be started");
        }

        tar.setStatus(TicketActionStatus.ADMIN_PROCESSING);
        tar.setFinalizedByAdminId(adminUserId);
        TicketActionRequest saved = ticketActionRequestRepository.save(tar);

        activityTicketActionAuditSupport.logProcessingStarted(saved, adminUserId);

        // Notify user that processing started
        if (saved.getRequester() != null) {
            String email = saved.getRequester().getEmail();
            notificationHelper.sendTicketActionProcessingToUser(
                    saved.getRequester().getId(),
                    email,
                    saved.getBooking().getPnr(),
                    saved.getType().name(),
                    saved.getId(),
                    email != null && !email.isBlank(),
                    saved.getBooking().getId()
            );
        }

        return toResponse(saved);
    }

    @Transactional
    public TicketActionRequestResponse adminFinalize(Long requestId, AdminTicketActionFinalizeRequest request, Long adminUserId) {
        TicketActionRequest tar = ticketActionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket action request"));

        if (tar.getStatus() != TicketActionStatus.USER_CONFIRMED && tar.getStatus() != TicketActionStatus.ADMIN_PROCESSING) {
            throw ServiceExceptions.accessDenied("Only USER_CONFIRMED or ADMIN_PROCESSING requests can be finalized");
        }

        if (request.getResultStatus() != TicketActionStatus.COMPLETED && request.getResultStatus() != TicketActionStatus.FAILED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "resultStatus must be COMPLETED or FAILED");
        }

        tar.setStatus(request.getResultStatus());
        tar.setFinalResult(request.getFinalResult());
        tar.setExternalReference(request.getExternalReference());
        tar.setFinalizedAt(UserDateTimeUtil.now());
        tar.setFinalizedByAdminId(adminUserId);

        TicketActionRequest saved;
        if (tar.getStatus() == TicketActionStatus.COMPLETED) {
            if (tar.getType() == TicketActionType.REISSUE) {
                saved = finalizeReissue(tar, request);
            } else {
                saved = finalizeRefundLike(tar, request);
            }
        } else {
            saved = ticketActionRequestRepository.save(tar);
        }

        activityTicketActionAuditSupport.logFinalized(
                saved,
                adminUserId,
                saved.getStatus() == TicketActionStatus.COMPLETED);

        // Notify user about final result
        if (saved.getRequester() != null) {
            String email = saved.getRequester().getEmail();
            boolean sendEmail = email != null && !email.isBlank();

            if (saved.getStatus() == TicketActionStatus.COMPLETED) {
                notificationHelper.sendTicketActionCompletedToUser(
                        saved.getRequester().getId(),
                        email,
                        saved.getBooking().getPnr(),
                        saved.getType().name(),
                        saved.getFinalResult(),
                        saved.getId(),
                        sendEmail,
                        saved.getBooking().getId()
                );
            } else if (saved.getStatus() == TicketActionStatus.FAILED) {
                notificationHelper.sendTicketActionFailedToUser(
                        saved.getRequester().getId(),
                        email,
                        saved.getBooking().getPnr(),
                        saved.getType().name(),
                        saved.getFinalResult(),
                        saved.getId(),
                        sendEmail
                );
            }
        }

        return toResponse(saved);

    }

    private TicketActionRequest finalizeReissue(
            TicketActionRequest tar,
            AdminTicketActionFinalizeRequest request) {
        if (tar.getQuoteTotalAmount() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Cannot complete reissue without a quoted total amount");
        }
        LocalDate reissueDate = request.getReissueDate() != null ? request.getReissueDate() : tar.getReissueDate();
        if (reissueDate == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "reissueDate is required when completing a reissue");
        }
        BigDecimal supplierReissueCost = request.getSupplierRefundCost();
        if (supplierReissueCost == null) {
            supplierReissueCost = tar.getQuoteAirlineCost();
        }
        if (supplierReissueCost == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "supplierRefundCost is required when completing a reissue");
        }

        Booking b = tar.getBooking();
        BigDecimal chargeAmount = tar.getQuoteTotalAmount();
        tar.setReissueDate(reissueDate);
        tar.setSupplierRefundCost(supplierReissueCost);
        tar.setSupplierPayableReversed(supplierReissueCost);
        tar.setRemainingSupplierPayable(null);
        tar.setRefunded(false);

        String reason = "Ticket action reissue completed"
                + (request.getFinalResult() != null && !request.getFinalResult().isBlank()
                ? ": " + request.getFinalResult() : "");

        bookingService.completeTicketActionReissue(
                b,
                chargeAmount,
                supplierReissueCost,
                reissueDate,
                tar.getId(),
                reason,
                request.getSegments(),
                tar.getFinalizedByAdminId());

        return ticketActionRequestRepository.save(tar);
    }

    private TicketActionRequest finalizeRefundLike(
            TicketActionRequest tar,
            AdminTicketActionFinalizeRequest request) {
        Booking b = tar.getBooking();
        if (tar.getQuoteTotalAmount() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Cannot complete ticket action without a quoted total amount");
        }
        BigDecimal supplierRefundCost = request.getSupplierRefundCost();
        if (supplierRefundCost == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "supplierRefundCost is required when completing a ticket action");
        }

        BigDecimal buyPrice = bookingService.resolveBookingBuyPrice(b);
        if (supplierRefundCost.compareTo(buyPrice) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "supplierRefundCost (" + supplierRefundCost + ") cannot exceed buy price (" + buyPrice + ")");
        }
        BigDecimal profitLoss = bookingService.resolveBookingProfitLoss(b);
        BigDecimal supplierPayableReversed = buyPrice.subtract(supplierRefundCost);
        tar.setSupplierRefundCost(supplierRefundCost);
        tar.setSupplierPayableReversed(supplierPayableReversed);
        tar.setRemainingSupplierPayable(supplierRefundCost);

        BigDecimal netProfitLoss = bookingService.computeNetProfitLossAfterRefund(
                profitLoss, supplierRefundCost, tar.getQuoteTotalAmount());

        Map<String, Object> refundMetadata = new LinkedHashMap<>();
        refundMetadata.put("source", "TICKET_ACTION");
        refundMetadata.put("ticketActionRequestId", tar.getId());
        refundMetadata.put("ticketActionType", tar.getType().name());
        refundMetadata.put("supplierRefundCost", supplierRefundCost);
        refundMetadata.put("buyPrice", buyPrice);
        refundMetadata.put("profitLoss", profitLoss);
        refundMetadata.put("netProfitLoss", netProfitLoss);
        refundMetadata.put("supplierPayableReversed", supplierPayableReversed);
        refundMetadata.put("remainingSupplierPayable", supplierRefundCost);

        BookingStatus newStatus = mapToBookingStatus(tar.getType());
        bookingService.updateBookingStatus(
                b.getId(),
                newStatus,
                "Ticket action completed: " + tar.getType(),
                tar.getQuoteTotalAmount(),
                refundMetadata);
        tar.setRefunded(true);
        return ticketActionRequestRepository.save(tar);
    }


    private BookingStatus mapToBookingStatus(TicketActionType type) {
        return switch (type) {
            case CANCEL -> BookingStatus.TICKET_CANCELLED;
            case VOID -> BookingStatus.VOID;
            case REFUND -> BookingStatus.REFUND;
            case REISSUE -> BookingStatus.REISSUE;
        };
    }

    private void autoRejectExpiredQuotes(Long bookingId) {
        LocalDateTime now = UserDateTimeUtil.now();

        // Load only quoted requests for this booking and expire those past deadline
        List<TicketActionRequest> quoted = ticketActionRequestRepository.findByBookingIdAndStatus(bookingId, TicketActionStatus.QUOTED);
        for (TicketActionRequest tar : quoted) {
            if (tar.getAcceptDeadline() != null && tar.getAcceptDeadline().isBefore(now)) {
                tar.setStatus(TicketActionStatus.REJECTED);
                tar.setAdminNote(
                        (tar.getAdminNote() != null ? tar.getAdminNote() + " | " : "") +
                                "Auto-rejected: user did not confirm within deadline"
                );
                ticketActionRequestRepository.save(tar);
                activityTicketActionAuditSupport.logRejected(tar, null, true);
            }
        }
    }

    private void enforceBookingAccess(Booking booking, Long currentUserId, boolean isAdmin) {
        if (isAdmin) return;

        boolean isOwner = booking.getCreatedBy().getId().equals(currentUserId);
        boolean isChildOfOwner = false;

        var currentUserOpt = userRepository.findById(currentUserId);
        if (currentUserOpt.isPresent() && currentUserOpt.get().getParentUser() != null) {
            isChildOfOwner = booking.getCreatedBy().getId().equals(currentUserOpt.get().getParentUser().getId());
        }

        if (!isOwner && !isChildOfOwner) {
            throw ServiceExceptions.accessDenied("Access denied");
        }
    }

    private TicketActionRequestResponse toResponse(TicketActionRequest tar) {
        Booking booking = tar.getBooking();
        User bookingOwner = booking.getCreatedBy();

        User agencyUser = bookingOwner != null && bookingOwner.getParentUser() != null
                ? bookingOwner.getParentUser()
                : bookingOwner;
        String agencyName = agencyUser != null ? agencyUser.getFullName() : null;

        String bookingPriceDisplay = booking.getBookingPrice();
        String bookingPriceCurrencyCode = null;
        if (agencyUser != null) {
            String agencyCurrency =
                    agencyUser.getCurrency() != null && !agencyUser.getCurrency().isBlank()
                            ? agencyUser.getCurrency()
                            : "USD";
            bookingPriceCurrencyCode = agencyCurrency;
            String raw = booking.getBookingPrice();
            if (raw != null && !raw.isBlank()) {
                String providerName = booking.getProviderName() != null
                        ? booking.getProviderName().name()
                        : "DEFAULT";
                try {
                    double amountUsd = Double.parseDouble(raw.trim());
                    double agencyRate =
                            currencyService.getExchangeRateBasedOnUsd(
                                    agencyCurrency, providerName, booking.getChannel());
                    double inAgency = amountUsd * agencyRate;
                    bookingPriceDisplay = BigDecimal.valueOf(inAgency)
                            .setScale(2, RoundingMode.HALF_UP)
                            .toPlainString();
                } catch (Exception e) {
                    log.warn(
                            "Could not convert booking price to agency currency for booking {}: {}",
                            booking.getId(),
                            e.getMessage());
                    bookingPriceDisplay = raw;
                }
            }
        }

        BigDecimal profitLoss = bookingService.resolveBookingProfitLoss(booking);
        BigDecimal buyPrice = bookingService.resolveBookingBuyPrice(booking);
        BigDecimal netProfitLoss = null;
        if (tar.getType() == TicketActionType.REISSUE) {
            if (tar.getQuoteTotalAmount() != null) {
                BigDecimal supplierCost = tar.getSupplierRefundCost() != null
                        ? tar.getSupplierRefundCost()
                        : tar.getQuoteAirlineCost();
                netProfitLoss = bookingService.computeNetProfitLossAfterReissue(
                        profitLoss, supplierCost, tar.getQuoteTotalAmount());
            }
        } else if (tar.getSupplierRefundCost() != null && tar.getQuoteTotalAmount() != null) {
            netProfitLoss = bookingService.computeNetProfitLossAfterRefund(
                    profitLoss, tar.getSupplierRefundCost(), tar.getQuoteTotalAmount());
        } else if (tar.getSupplierRefundCost() != null) {
            netProfitLoss = bookingService.computeNetProfitLossAfterRefund(
                    profitLoss, tar.getSupplierRefundCost(), BigDecimal.ZERO);
        }

        return TicketActionRequestResponse.builder()
                .id(tar.getId())
                .bookingId(booking.getId())
                .pnr(booking.getPnr())
                .ticketNo(booking.getTicketNo())
                .agencyName(agencyName)
                .bookingPrice(bookingPriceDisplay)
                .bookingPriceCurrency(bookingPriceCurrencyCode)
                .buyPrice(buyPrice)
                .profitLoss(profitLoss)
                .netProfitLoss(netProfitLoss)
                .type(tar.getType())
                .status(tar.getStatus())
                .reason(tar.getReason())
                .adminNote(tar.getAdminNote())
                .airlineCost(tar.getQuoteAirlineCost())
                .serviceCharge(tar.getQuoteServiceCharge())
                .totalAmount(tar.getQuoteTotalAmount())
                .currency(tar.getQuoteCurrency())
                .details(tar.getQuoteDetails())
                .createdAt(timestampMapper.toRequestUserTime(tar.getCreatedAt(), tar.getCreatedTimeOffset()))
                .quotedAt(timestampMapper.toRequestUserTime(tar.getQuotedAt(), tar.getCreatedTimeOffset()))
                .userConfirmedAt(timestampMapper.toRequestUserTime(tar.getUserConfirmedAt(), tar.getCreatedTimeOffset()))
                .finalizedAt(timestampMapper.toRequestUserTime(tar.getFinalizedAt(), tar.getCreatedTimeOffset()))
                .externalReference(tar.getExternalReference())
                .finalResult(tar.getFinalResult())
                .acceptDeadline(tar.getAcceptDeadline())
                .quoteExchangeRate(tar.getQuoteExchangeRate())
                .quoteUserCurrency(tar.getQuoteUserCurrency())
                .refundTimeline(tar.getRefundTimeline())
                .refunded(tar.isRefunded())
                .supplierRefundCost(tar.getSupplierRefundCost())
                .supplierPayableReversed(tar.getSupplierPayableReversed())
                .remainingSupplierPayable(tar.getRemainingSupplierPayable())
                .reissueDate(tar.getReissueDate())
                .reissueChargeAmount(
                        tar.getType() == TicketActionType.REISSUE && tar.getStatus() == TicketActionStatus.COMPLETED
                                ? tar.getQuoteTotalAmount()
                                : null)
                .build();
    }
}
