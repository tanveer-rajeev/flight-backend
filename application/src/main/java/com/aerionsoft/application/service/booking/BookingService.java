package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.context.UserTimezoneContext;
import com.aerionsoft.application.dto.admin.GroupTicket.GroupTicketDTO;
import com.aerionsoft.application.dto.admin.summery.LastTenBookings;
import com.aerionsoft.application.dto.agency.AgencyInfo;
import com.aerionsoft.application.dto.booking.*;
import com.aerionsoft.application.dto.booking.TravelInformation;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.business.BusinessSimpleDto;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.dto.common.TravellerShortDto;
import com.aerionsoft.application.dto.flight.StatusChangeRequest;
import com.aerionsoft.application.dto.ticketaction.ReissueSegmentDateUpdate;
import com.aerionsoft.application.dto.traveller.TravellerResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.Booking.BookingPackageBaggage;
import com.aerionsoft.application.entity.Booking.Extras;
import com.aerionsoft.application.entity.Booking.Traveller;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.group.*;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.booking.BookingClass;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.enums.group.GroupTicketType;
import com.aerionsoft.application.enums.notification.NotificationPriority;
import com.aerionsoft.application.enums.notification.NotificationType;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.event.BookingCreatedNotificationEvent;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.interafces.BookingInterface;
import com.aerionsoft.application.interafces.UserInterface;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.repository.booking.*;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.group.GroupTicketRepository;
import com.aerionsoft.application.repository.payment.PaymentRepository;
import com.aerionsoft.application.repository.payment.SslCommerzPaymentRepository;
import com.aerionsoft.application.repository.spec.OffsetAwareDateSpec;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.audit.ActivityBookingAuditSupport;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.service.notification.NotificationHelper;
import com.aerionsoft.application.service.wallet.CreditLimitValidatorService;
import com.aerionsoft.application.service.wallet.ReferenceGeneratorService;
import com.aerionsoft.application.util.Helper;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService implements BookingInterface {

    @Autowired
    private TravellerService travellerService;

    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private TravelInformationRepository travelInformationRepository;

    @Autowired
    private BookingSegmentRepository bookingSegmentRepository;

    @Autowired
    private SegmentAirportRepository segmentAirportRepository;

    @Autowired
    private SegmentAirlineRepository segmentAirlineRepository;

    private final BookingRepository bookingRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ExtrasRepository extrasRepository;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private BookingPackageBaggageRepository bookingPackageBaggageRepository;

    @Autowired
    private BookingTravellerTicketRepository bookingTravellerTicketRepository;


    @Autowired
    private UserInterface userService;

    @Autowired
    private WalletDepositRepository walletDepositRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ReferenceGeneratorService referenceGeneratorService;
    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private BookingSupplierInvoiceService bookingSupplierInvoiceService;

    @Autowired
    private NotificationHelper notificationHelper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private BookingTimelineService bookingTimelineService;

    @Autowired
    private ActivityBookingAuditSupport activityBookingAuditSupport;

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(BookingService.class.getName());

    @Autowired
    private GroupTicketRepository groupTicketRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SslCommerzPaymentRepository sslCommerzPaymentRepository;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @Autowired
    private BookingTimelineRepository bookingTimelineRepository;

    @Autowired
    private TimestampMapper timestampMapper;

    @Autowired
    private BusinessRepository businessRepository;

    public BookingService(BookingRepository bookingRepo
    ) {
        this.bookingRepo = bookingRepo;

    }

    @Override
    public BookingResponse create(BookingRequest req, Long userId, List<Long> travellerIds, BookConformation bookConformation,
                                  String price, Long actingUserId, String originalPrice, String buyPrice, String markupAmount,
                                  User user, double exchangeRate, String userCurrency) {

        String profitLoss = computeProfitLossUsd(price, buyPrice, originalPrice);

        Booking booking = Booking.builder()
                .providerName(req.getProviderName())
                .bookingClass(String.valueOf(req.getBookingClass().getValue()))
                .type(req.getType())
                .description(req.getDescription())
                .createdBy(user)
                .actingUserId(actingUserId)
                .bookingDate(bookConformation.getBookingDate())
                .bookingPrice(price)
                .exchangeCurrencyRate(String.valueOf(exchangeRate))
                .exchangeCurrency(userCurrency)
                .originalPrice(originalPrice)
                .buyPrice(buyPrice)
                .profitLoss(profitLoss)
                .markupAmount(markupAmount)
                .pnr(bookConformation.getPnr())
                .ticketNo(bookConformation.getTicketNo())
                .airline(bookConformation.getAirline())
                .reason(bookConformation.getReason())
                .status(bookConformation.getStatus())
                .createdByName(user.getFullName())
                .createdAt(UserDateTimeUtil.now())
                .createdTimeOffset(UserDateTimeUtil.currentOffset())
                .bookingAllowed(Boolean.TRUE.equals(req.getIsBookingAllowed()))
                .ticketingAllowed(Boolean.TRUE.equals(req.getIsTicketingAllowed()))
                .taxAmount(req.getFare().getTax())
                .updatedAt(UserDateTimeUtil.now())
                .updatedTimeOffset(UserDateTimeUtil.currentOffset())
                .airlinePnrs(bookConformation.getAirlinePnrs())
                .lastPaymentDateInSeconds(bookConformation.getLastPaymentDateInSeconds())
                .tripType(req.getTripType())
                .channel(bookConformation.getChannel())
                .lastPaymentDate(
                        bookConformation != null ? bookConformation.getLastPaymentDate() : null
                )
                .brandCurrency(
                        req != null && req.getFare() != null
                                ? req.getFare().getBaseFareCurrency()
                                : null
                )
                .brandExchangeRate(
                        req != null && req.getFare() != null
                                ? req.getFare().getFareExchangeRate()
                                : null
                )

                .travellerIds(travellerIds.isEmpty() ? null :
                        travellerIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .travellerId(travellerIds.isEmpty() ? null : travellerIds.get(0))
                .bookingReference(referenceGeneratorService.nextReference("FR"))
                .bundleCode(req.getBundleCode())
                .providerBookingTime(bookConformation.getProviderBookingTime())
                .timeOffset(req.getTimeOffset())
                .bookedTimeOffset(bookConformation.getBookedTimeOffset())
                .sourceType(bookConformation.getSourceType())
                .groupTicketType(resolveGroupTicketType(req))
                .build();


        try {
            bookingRepo.save(booking);

            // Save package baggage list
            if (req.getPackageBaggageList() != null && !req.getPackageBaggageList().isEmpty()) {
                for (var packageBaggage : req.getPackageBaggageList()) {
                    BookingPackageBaggage bookingPackageBaggage = BookingPackageBaggage.builder()
                            .bookingId(booking.getId())
                            .pax(packageBaggage.getPax())
                            .weight(packageBaggage.getWeight())
                            .unit(packageBaggage.getUnit())
                            .flightNumber(packageBaggage.getFlightNumber())
                            .build();
                    bookingPackageBaggageRepository.save(bookingPackageBaggage);
                }
            }

            // Prepare extras list to be associated with booking
            if (bookConformation.getFlightDetails() != null) {
                for (var flightDetail : bookConformation.getFlightDetails()) {
                    Extras extras = new Extras();
                    extras.setBookingId(booking.getId());
                    extras.setFlightNumber(flightDetail.getFlightNumber());

                    String[] baggageCodes = flightDetail.getSsrRequest().getBaggageCode();
                    if (baggageCodes != null && baggageCodes.length > 0) {
                        extras.setBaggageCode(String.join(",", baggageCodes));
                    }
                    String[] mealCodes = flightDetail.getSsrRequest().getMealCode();
                    if (mealCodes != null && mealCodes.length > 0) {
                        extras.setMealCode(String.join(",", mealCodes));
                    }
                    String[] seatCodes = flightDetail.getSsrRequest().getSeatCode();
                    if (seatCodes != null && seatCodes.length > 0) {
                        extras.setSeatCode(String.join(",", seatCodes));
                    }
                    extrasRepository.save(extras);
                }
            }
        } catch (Exception ex) {
            throw ServiceExceptions.notFound("Failed to create booking: " + ex.getMessage());
        }

        // Record timeline event for booking creation
        bookingTimelineService.record(
                booking.getId(),
                booking.getStatus() != null ? booking.getStatus() : BookingStatus.PROCESS,
                null,
                booking.getPnr(),
                booking.getTicketNo(),
                booking.getReason(),
                userId,
                user.getFullName(),
                "USER"
        );

        activityBookingAuditSupport.logBookingCreated(
                booking.getId(),
                booking.getPnr(),
                booking.getStatus(),
                booking.getSourceType(),
                userId,
                req.getProviderName() != null ? req.getProviderName().name() : null);

        eventPublisher.publishEvent(new BookingCreatedNotificationEvent(
                userId,
                user.getEmail(),
                user.getFullName(),
                booking.getBookingReference(),
                booking.getPnr(),
                booking.getExchangeCurrency() != null ? booking.getExchangeCurrency() : "USD",
                booking.getId()
        ));

        return mapToResponse(booking);

    }


    public Booking getBookingById(Long id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    public BookingResponse update(Long id, BookingRequest req) {
        Booking b = bookingRepo.findById(id).orElseThrow();

        b.setProviderName(req.getProviderName());
        b.setBookingClass(String.valueOf(req.getBookingClass().getValue()));
        b.setType(req.getType());
        b.setDescription(req.getDescription());

        // Only update traveller IDs if provided (don't create new travellers)
        if (req.getTravellerIds() != null) {
            b.setTravellerIds(req.getTravellerIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            b.setTravellerId(!req.getTravellerIds().isEmpty() ? req.getTravellerIds().get(0) : null);
        }

        b.setUpdatedAt(UserDateTimeUtil.now());
        b.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        bookingRepo.save(b);
        activityBookingAuditSupport.logBookingUpdated(b.getId(), b.getPnr());
        return mapToResponse(b);
    }


    @Transactional
    public BookingDeleteResponse delete(Long id) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking"));
        Long ownerId = booking.getCreatedBy().getId();

        List<Transaction> bookingTransactions = transactionRepository.findBySourceTypeAndSourceId(
                TransactionSourceType.BOOKING.name(), id);
        boolean hadWalletPurchase = bookingTransactions.stream()
                .anyMatch(t -> DepositType.PURCHASE.name().equals(t.getType()));

        Double walletRefundAmount = null;
        if (hadWalletPurchase) {
            User user = booking.getCreatedBy();
            Long userId = user.getId();

            List<Double> purchaseAmounts = bookingTransactions.stream()
                    .filter(t -> DepositType.PURCHASE.name().equals(t.getType()))
                    .map(t -> t.getAmount() != null ? t.getAmount() : 0.0)
                    .filter(amount -> amount > 0)
                    .toList();

            if (!purchaseAmounts.isEmpty()) {
                userService.silentlyUndoBookingWalletDebits(userId, booking.getId(), purchaseAmounts);
                walletRefundAmount = purchaseAmounts.stream().mapToDouble(Double::doubleValue).sum();
            }
        }

        List<Long> depositIdsToRemove = bookingTransactions.stream()
                .map(Transaction::getReference)
                .filter(java.util.Objects::nonNull)
                .map(walletDepositRepository::findByReference)
                .filter(java.util.Optional::isPresent)
                .map(opt -> opt.get().getId())
                .distinct()
                .toList();

        transactionRepository.deleteAll(bookingTransactions);
        for (Long depositId : depositIdsToRemove) {
            walletDepositRepository.deleteById(depositId);
        }

        paymentRepository.deleteByBookingId(id);
        sslCommerzPaymentRepository.deleteAll(sslCommerzPaymentRepository.findByBookingId(id));
        bookingTimelineRepository.deleteByBookingId(id);
        bookingTravellerTicketRepository.deleteByBookingId(id);
        extrasRepository.deleteByBookingId(id);
        bookingPackageBaggageRepository.deleteByBookingId(id);

        com.aerionsoft.application.entity.group.TravelInformation travelInformation = travelInformationRepository.findByBookingId(id);
        if (travelInformation != null) {
            List<BookingSegment> segments = bookingSegmentRepository.findByTravelInformationId(travelInformation.getId());
            for (BookingSegment segment : segments) {
                segmentAirportRepository.deleteBySegmentId(segment.getId());
                segmentAirlineRepository.deleteBySegmentId(segment.getId());
            }
            bookingSegmentRepository.deleteByTravelInformationId(travelInformation.getId());
            travelInformationRepository.delete(travelInformation);
        }

        activityBookingAuditSupport.logBookingDeleted(booking.getId(), booking.getPnr());

        bookingRepo.delete(booking);

        double balanceAfter = userService.getUserBalance(ownerId);
        return BookingDeleteResponse.builder()
                .walletRefundAmount(walletRefundAmount)
                .balanceAfter(balanceAfter)
                .build();
    }

    public Page<BookingResponse> search(
            String currencyCode, LocalDate fromDate, LocalDate toDate, String pnrOrId, String ticketNo, BookingStatus status,
            int page, int size, boolean isAdmin, Long userId, String sourceType) {

        final String currency;
        if (isAdmin && currencyCode != null && !currencyCode.isBlank()) {
            try {
                currency = Currency.valueOf(currencyCode.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid currency: " + currencyCode);
            }
        } else {
            currency = null;
        }

        Specification<Booking> spec = null;
        ZoneId userZone = UserTimezoneContext.getZoneId();

        if (fromDate != null && toDate != null) {
            OffsetDateTime from = fromDate.atStartOfDay(userZone).toOffsetDateTime();
            OffsetDateTime to = toDate.atTime(23, 59, 59).atZone(userZone).toOffsetDateTime();
            spec = (root, query, cb) -> cb.between(root.get("bookingDate"), from, to);
        } else if (fromDate != null) {
            OffsetDateTime from = fromDate.atStartOfDay(userZone).toOffsetDateTime();
            spec = (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("bookingDate"), from);
        } else if (toDate != null) {
            OffsetDateTime to = toDate.atTime(23, 59, 59).atZone(userZone).toOffsetDateTime();
            spec = (root, query, cb) -> cb.lessThanOrEqualTo(root.get("bookingDate"), to);
        }

        if (pnrOrId != null && !pnrOrId.isEmpty()) {
            Specification<Booking> cond = (root, query, cb) ->
                    cb.or(
                            cb.equal(root.get("pnr"), pnrOrId),
                            cb.equal(root.get("id"), tryParseLong(pnrOrId))
                    );
            spec = (spec == null) ? cond : spec.and(cond);
        }

        if (ticketNo != null && !ticketNo.isEmpty()) {
            Specification<Booking> cond = (root, query, cb) -> cb.equal(root.get("ticketNo"), ticketNo);
            spec = (spec == null) ? cond : spec.and(cond);
        }

        if (status != null && status != BookingStatus.ALL) {
            Specification<Booking> cond = (root, query, cb) -> cb.equal(root.get("status"), status);
            spec = (spec == null) ? cond : spec.and(cond);
        }

        if (isAdmin && currency != null) {
            Specification<Booking> cond = (root, query, cb) ->
                    cb.equal(cb.upper(root.get("createdBy").get("currency")), currency);
            spec = (spec == null) ? cond : spec.and(cond);
        }

        if (!isAdmin && userId != null) {
            Specification<Booking> cond = (root, query, cb) ->
                    cb.equal(root.get("createdBy").get("id"), userId);
            spec = (spec == null) ? cond : spec.and(cond);
        }

        if (sourceType != null && !sourceType.isBlank()) {
            if ("ONLINE".equalsIgnoreCase(sourceType)) {
                Specification<Booking> cond = (root, query, cb) -> cb.or(
                        cb.equal(cb.upper(root.get("sourceType")), "ONLINE"),
                        cb.isNull(root.get("sourceType"))
                );
                spec = (spec == null) ? cond : spec.and(cond);
            } else {
                Specification<Booking> cond = (root, query, cb) ->
                        cb.equal(cb.upper(root.get("sourceType")), sourceType.toUpperCase());
                spec = (spec == null) ? cond : spec.and(cond);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Booking> bookings = (spec == null)
                ? bookingRepo.findAll(pageable)
                : bookingRepo.findAll(spec, pageable);

        List<BookingResponse> optimizedResponses = mapToResponses(bookings.getContent());

        // Create a new page with the optimized responses
        return new org.springframework.data.domain.PageImpl<>(
                optimizedResponses,
                pageable,
                bookings.getTotalElements()
        );
    }


    private long tryParseLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception ex) {
            return -1;
        }
    }

    public BookingResponse mapToResponse(Booking b) {
        return mapToResponseOptimized(b, null, null);
    }

    // Optimized version with batch data
    private BookingResponse mapToResponseOptimized(Booking b,
                                                   java.util.Map<Long, TravellerResponse> travellerCache,
                                                   java.util.Map<String, GroupTicketDTO> groupTicketCache) {

        TravelInformation travelInformation = null;
        List<Long> travellerIds = new ArrayList<>();
        List<TravellerResponse> travellers = new ArrayList<>();

        // Collect all traveller IDs first
        if (b.getTraveller() != null) {
            TravellerResponse t = travellerService.mapToResponse(b.getTraveller());
            travellerIds.add(t.getId());
            travellers.add(t);
        } else if (b.getTravellerId() != null) {
            travellerIds.add(b.getTravellerId());
        }

        // Add multiple travellers from travellerIds string
        if (b.getTravellerIds() != null && !b.getTravellerIds().isEmpty()) {
            List<Long> allTravellerIds = Helper.parseIds(b.getTravellerIds());
            for (Long travellerId : allTravellerIds) {
                if (!travellerIds.contains(travellerId)) {
                    travellerIds.add(travellerId);
                }
            }
        }

        // Load travellers efficiently using cache or batch loading
        if (!travellerIds.isEmpty()) {
            if (travellerCache != null) {
                // Use cached data
                for (Long travellerId : travellerIds) {
                    TravellerResponse traveller = travellerCache.get(travellerId);
                    if (traveller != null) {
                        travellers.add(traveller);
                    }
                }
            } else {
                // Batch load if no cache provided
                try {
                    List<TravellerResponse> batchTravellers = travellerService.getTravellersByIds(travellerIds);
                    travellers.addAll(batchTravellers);
                } catch (Exception e) {
                    // Fallback to individual loading if batch fails
                    for (Long travellerId : travellerIds) {
                        try {
                            TravellerResponse t = travellerService.getTravellerById(travellerId);
                            travellers.add(t);
                        } catch (Exception ex) {
                            // Skip if traveller not found
                        }
                    }
                }
            }
        }

        com.aerionsoft.application.entity.group.TravelInformation travelInformation1 = travelInformationRepository.findByBookingId(b.getId());
        if (travelInformation1 != null) {
            travelInformation = buildTravelInformation(travelInformation1);
        }


        travellers = new ArrayList<>(travellers.stream()
                .collect(Collectors.toMap(
                        TravellerResponse::getId,
                        traveller -> traveller,
                        (existing, replacement) -> existing))
                .values());

        // Stamp per-booking ticket numbers onto each traveller
        try {
            java.util.Map<Long, String> ticketMap = bookingTravellerTicketRepository.getTicketMapForBooking(b.getId());
            if (!ticketMap.isEmpty()) {
                travellers.forEach(t -> t.setTicketNumber(ticketMap.get(t.getId())));
            }
        } catch (Exception e) {
            log.warning("Could not load traveller ticket numbers for booking " + b.getId() + ": " + e.getMessage());
        }

        List<Extras> extrasList = extrasRepository.findByBookingId(b.getId());
        List<ExtrasDTO> extrasDTO = mapExtrasList(extrasList);

        List<BookingPackageBaggage> packageBaggageList = bookingPackageBaggageRepository.findByBookingId(b.getId());
        List<PackageBaggageDTO> packageBaggageDTOList = mapPackageBaggageList(packageBaggageList);

        BusinessSimpleDto businessSimpleDto = getBusinessSimpleDto(b.getCreatedBy().getId());
        return BookingResponse.builder()
                .id(b.getId())
                .providerName(b.getProviderName())
                .bookingClass(BookingClass.fromValue(Integer.parseInt(b.getBookingClass().toUpperCase())))
                .type(b.getType())
                .bookingDate(b.getBookingDate())
                .pnr(b.getPnr())
                .ticketNo(b.getTicketNo())
                .ticketNumbers(convertAirlinePnrsToList(b.getTicketNo()))
                .description(b.getDescription())
                .createdByName(b.getCreatedByName())
                .airline(b.getAirline())
                .status(b.getStatus())
                .createdBy(b.getCreatedBy().getId())
                .lastPaymentDate(b.getLastPaymentDate())
                .markupAmount(b.getMarkupAmount())
                .originalPrice(b.getOriginalPrice())
                .buyPrice(b.getBuyPrice())
                .profitLoss(resolveProfitLoss(b))
                .bookingPrice(b.getBookingPrice())
                .createdAt(timestampMapper.toRequestUserTime(
                        b.getCreatedAt(),
                        b.getCreatedTimeOffset() != null ? b.getCreatedTimeOffset() : b.getTimeOffset()))
                .exchangeCurrencyRate(b.getExchangeCurrencyRate())
                .exchangeCurrency(b.getExchangeCurrency())
                .updatedAt(timestampMapper.toRequestUserTime(
                        b.getUpdatedAt(),
                        b.getUpdatedTimeOffset() != null ? b.getUpdatedTimeOffset() : b.getTimeOffset()))
                .travellerIds(travellerIds)
                .travellers(travellers)
                .business(businessSimpleDto)
                .TripType(b.getTripType() != null ? b.getTripType().name() : null)
                .travelInformation(travelInformation)
                .extras(extrasDTO)
                .bookingPrice(b.getBookingPrice())
                .originalPrice(b.getOriginalPrice())
                .buyPrice(b.getBuyPrice())
                .profitLoss(resolveProfitLoss(b))
                .markupAmount(b.getMarkupAmount())
                .isBookingAllowed(b.isBookingAllowed())
                .isTicketingAllowed(b.isTicketingAllowed())
                .taxAmount(b.getTaxAmount())
                .user(userMap(b.getCreatedBy()))
                .brandExchangeRate(b.getBrandExchangeRate())
                .brandCurrency(b.getBrandCurrency())
                .bookingReference(b.getBookingReference())
                .bundleCode(b.getBundleCode())
                .packageBaggageList(packageBaggageDTOList)
                .airlinePnrs(convertAirlinePnrsToList(b.getAirlinePnrs()))
                .providerBookingTime(b.getProviderBookingTime())
                .timeOffset(b.getTimeOffset())
                .bookedTimeOffset(b.getBookedTimeOffset())
                .channel(b.getChannel())
                .sourceType(b.getSourceType())
                .groupTicketType(b.getGroupTicketType())
                .importedPnr(Boolean.TRUE.equals(b.getImportedPnr()))
                .build();
    }

    private String resolveGroupTicketType(BookingRequest req) {
        if (req.getGroupTicketType() == null || req.getGroupTicketType().isBlank()) {
            return null;
        }
        return GroupTicketType.fromValue(req.getGroupTicketType()).getValue();
    }

    static String computeProfitLossUsd(String sellPrice, String buyPrice, String originalPrice) {
        double sell = parseStoredUsdPrice(sellPrice);
        double buy = parseStoredUsdPrice(buyPrice);
        if (buy <= 0) {
            buy = parseStoredUsdPrice(originalPrice);
        }
        return formatUsdPrice(sell - buy);
    }

    private String resolveProfitLoss(Booking booking) {
        if (booking.getProfitLoss() != null && !booking.getProfitLoss().isBlank()) {
            return booking.getProfitLoss();
        }
        return computeProfitLossUsd(booking.getBookingPrice(), booking.getBuyPrice(), booking.getOriginalPrice());
    }

    private static double parseStoredUsdPrice(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String formatUsdPrice(double amount) {
        return java.math.BigDecimal.valueOf(amount)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private UserDto userMap(User user) {
        if (user == null) {
            return null;
        }
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .code(user.getCode())
                .build();
    }

    private List<String> convertAirlinePnrsToList(String airlinePnrs) {
        if (airlinePnrs == null || airlinePnrs.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(airlinePnrs.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Optimized batch processing for multiple bookings
    private List<BookingResponse> mapToResponses(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return new ArrayList<>();
        }

        // Collect all unique traveller IDs
        java.util.Set<Long> allTravellerIds = new java.util.HashSet<>();
        for (Booking booking : bookings) {
            if (booking.getTravellerId() != null) {
                allTravellerIds.add(booking.getTravellerId());
            }
            if (booking.getTravellerIds() != null && !booking.getTravellerIds().isEmpty()) {
                allTravellerIds.addAll(Helper.parseIds(booking.getTravellerIds()));
            }
        }

        // Batch load all travellers
        java.util.Map<Long, TravellerResponse> travellerCache = new java.util.HashMap<>();
        if (!allTravellerIds.isEmpty()) {
            try {
                List<TravellerResponse> batchTravellers = travellerService.getTravellersByIds(new ArrayList<>(allTravellerIds));
                for (TravellerResponse traveller : batchTravellers) {
                    travellerCache.put(traveller.getId(), traveller);
                }
            } catch (Exception e) {
                // If batch loading fails, cache will remain empty and individual loading will be used
            }
        }

        // Collect all unique PNRs for group tickets
        java.util.Set<String> allPnrs = new java.util.HashSet<>();
        for (Booking booking : bookings) {
            if (booking.getPnr() != null && "Group".equalsIgnoreCase(String.valueOf(booking.getProviderName()))) {
                allPnrs.add(booking.getPnr());
            }
        }

        // Map all bookings using the cached data
        return bookings.stream()
                .map(booking -> mapToResponseOptimized(booking, travellerCache, null))
                .collect(java.util.stream.Collectors.toList());
    }


    private BusinessSimpleDto getBusinessSimpleDto(Long userId) {
        try {
            BusinessDto businessDto = businessService.getBusinessByUserId(userId);
            return BusinessSimpleDto.builder()
                    .companyAddress(businessDto.getCompanyAddress())
                    .companyName(businessDto.getCompanyName())
                    .companyEmail(businessDto.getCompanyEmail())
                    .companyPhone(businessDto.getCompanyPhone())
                    .companyLogo(businessDto.getCompanyLogo())
                    .motherUserId(businessDto.getMotherUserId())
                    .motherUserFullName(businessDto.getMotherUserFullName())
                    .id(businessDto.getId())
                    .build();
        } catch (Exception e) {
            return null;
        }
    }


    public String totalBookingsCount() {
        return String.valueOf(bookingRepo.count());
    }
    public long totalIssuedCount() {
        return bookingRepo.countByStatus(BookingStatus.CONFIRMED);
    }

    public String last7DaysBookingsCount() {
        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        return String.valueOf(countByStatusInUserDateRange(BookingStatus.PNR, today.minusDays(6), today));
    }

    public long last7DaysIssuedCount() {
        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        return countByStatusInUserDateRange(BookingStatus.CONFIRMED, today.minusDays(6), today);
    }

    public String todayBookingsCount() {
        return String.valueOf(countByStatusOnUserDate(BookingStatus.PNR, UserDateTimeUtil.now().toLocalDate()));
    }

    public String todayIssueCount() {
        return String.valueOf(countByStatusOnUserDate(BookingStatus.CONFIRMED, UserDateTimeUtil.now().toLocalDate()));
    }

    public String todayRefundCount() {
        return String.valueOf(countByStatusOnUserDate(BookingStatus.REFUND, UserDateTimeUtil.now().toLocalDate()));
    }

    public String last7DaysRefundCount() {
        LocalDate today = UserDateTimeUtil.now().toLocalDate();
        return String.valueOf(countByStatusInUserDateRange(BookingStatus.REFUND, today.minusDays(6), today));
    }

    public long countPortalIssuedOnUserDate(LocalDate date) {
        return countPortalIssuedInUserDateRange(date, date);
    }

    public long countPortalIssuedInUserDateRange(LocalDate from, LocalDate to) {
        Specification<Booking> spec = portalIssuedSpec();
        Specification<Booking> dateSpec = OffsetAwareDateSpec.createdAtInUserRange(
                from, to, "createdAt", "createdTimeOffset", "timeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return bookingRepo.count(spec);
    }

    private long countByStatusOnUserDate(BookingStatus status, LocalDate date) {
        Specification<Booking> spec = (root, query, cb) -> cb.equal(root.get("status"), status);
        Specification<Booking> dateSpec = OffsetAwareDateSpec.createdAtOnUserDate(
                date, "createdAt", "createdTimeOffset", "timeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return bookingRepo.count(spec);
    }

    private long countByStatusInUserDateRange(BookingStatus status, LocalDate from, LocalDate to) {
        Specification<Booking> spec = (root, query, cb) -> cb.equal(root.get("status"), status);
        Specification<Booking> dateSpec = OffsetAwareDateSpec.createdAtInUserRange(
                from, to, "createdAt", "createdTimeOffset", "timeOffset");
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return bookingRepo.count(spec);
    }

    private Specification<Booking> portalIssuedSpec() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), BookingStatus.CONFIRMED),
                cb.or(
                        cb.equal(cb.upper(root.get("sourceType")), "ONLINE"),
                        cb.isNull(root.get("sourceType"))
                )
        );
    }

    @Override
    public List<BookingResponse> getAllBookingsByUserId(Long userId) {
        List<Booking> bookings = bookingRepo.findByCreatedBy(userId);
        return mapToResponses(bookings); // Use optimized batch processing
    }


    public List<LastTenBookings> getLastTenBookings() {

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookings = bookingRepo.findAll(pageable);

        return bookings.getContent().stream().map(this::mapBookDto).collect(Collectors.toList());

    }

    @Override
    public HashMap<String, Integer> getBookingCountByStatus(Long userId) {
        HashMap<String, Integer> statusCount = new HashMap<>();
        List<Object[]> results = bookingRepo.countBookingsByStatusForUser(userId);
        for (Object[] row : results) {
//            String status = (String) row[0];
            BookingStatus bookingStatus = (BookingStatus) row[0];
            Integer count = ((Number) row[1]).intValue();
            statusCount.put(String.valueOf(bookingStatus), count);
        }
        // Ensure all statuses are present, even if count is zero
        for (BookingStatus status : BookingStatus.values()) {
            statusCount.putIfAbsent(status.name(), 0);
        }
        return statusCount;
    }

    @Override
    public HashMap<String, Collection<Long>> getTravellerIdsByStatus(Long userId) {
        HashMap<String, Collection<Long>> travellerIdsByStatus = new HashMap<>();
        List<Booking> bookings = bookingRepo.findByCreatedBy(userId);

        for (Booking booking : bookings) {
            String status = booking.getStatus().name();
            // Handle null traveller case
            if (booking.getTraveller() != null) {
                travellerIdsByStatus
                        .computeIfAbsent(status, k -> new java.util.ArrayList<>())
                        .add(booking.getTraveller().getId());
            } else if (booking.getTravellerId() != null) {
                // Use travellerId directly if traveller entity is null but ID exists
                travellerIdsByStatus
                        .computeIfAbsent(status, k -> new java.util.ArrayList<>())
                        .add(booking.getTravellerId());
            }
            // If both traveller and travellerId are null, skip this booking
        }

        return travellerIdsByStatus;
    }

    public BookingResponse getById(Long id, Long userId, boolean isAdmin) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking"));
        TravelInformation travelInformation = null;

        // Access control - allow if admin, owner, or child of owner
        if (!isAdmin) {
            boolean isOwner = booking.getCreatedBy().getId().equals(userId);
            boolean isChildOfOwner = false;

            // Check if current user is a child of the booking creator
            var currentUser = userRepo.findById(userId);
            if (currentUser.isPresent() && currentUser.get().getParentUser() != null) {
                isChildOfOwner = booking.getCreatedBy().getId().equals(currentUser.get().getParentUser().getId());
            }

            if (!isOwner && !isChildOfOwner) {
                throw ServiceExceptions.notFound("Access denied");
            }
        }

        List<Long> travellerIds = new ArrayList<>();
        List<TravellerResponse> travellers = new ArrayList<>();

        if (booking.getTravellerIds() != null) {
            List<Long> allTravellerIds = Helper.parseIds(booking.getTravellerIds());
            for (Long travellerId : allTravellerIds) {
                TravellerResponse t = travellerService.getTravellerById(travellerId);
                travellerIds.add(t.getId());
                travellers.add(t);
            }
        }

        // Stamp per-booking ticket numbers onto each traveller
        try {
            java.util.Map<Long, String> ticketMap = bookingTravellerTicketRepository.getTicketMapForBooking(booking.getId());
            if (!ticketMap.isEmpty()) {
                travellers.forEach(t -> t.setTicketNumber(ticketMap.get(t.getId())));
            }
        } catch (Exception e) {
            log.warning("Could not load traveller ticket numbers for booking " + booking.getId() + ": " + e.getMessage());
        }

        // Always load travel information with segments if it exists
        com.aerionsoft.application.entity.group.TravelInformation travelInformation1 = travelInformationRepository.findByBookingId(booking.getId());
        if (travelInformation1 != null) {
            travelInformation = buildTravelInformation(travelInformation1);
        }

        List<Extras> extrasList = extrasRepository.findByBookingId(booking.getId());
        List<ExtrasDTO> extrasDTO = mapExtrasList(extrasList);

        List<BookingPackageBaggage> packageBaggageList = bookingPackageBaggageRepository.findByBookingId(booking.getId());
        List<PackageBaggageDTO> packageBaggageDTOList = mapPackageBaggageList(packageBaggageList);

        BusinessSimpleDto businessSimpleDto = getBusinessSimpleDto(booking.getCreatedBy().getId());

        return BookingResponse.builder()
                .id(booking.getId())
                .providerName(booking.getProviderName())
                .bookingClass(BookingClass.fromValue(Integer.parseInt(booking.getBookingClass())))
                .type(booking.getType())
                .bookingDate(booking.getBookingDate())
                .createdByName(booking.getCreatedByName())
                .pnr(booking.getPnr())
                .ticketNo(booking.getTicketNo())
                .ticketNumbers(convertAirlinePnrsToList(booking.getTicketNo()))
                .description(booking.getDescription())
                .airline(booking.getAirline())
                .status(booking.getStatus())
                .createdBy(booking.getCreatedBy().getId())
                .createdAt(timestampMapper.toRequestUserTime(
                        booking.getCreatedAt(),
                        booking.getCreatedTimeOffset() != null ? booking.getCreatedTimeOffset() : booking.getTimeOffset()))
                .updatedAt(timestampMapper.toRequestUserTime(
                        booking.getUpdatedAt(),
                        booking.getUpdatedTimeOffset() != null ? booking.getUpdatedTimeOffset() : booking.getTimeOffset()))
                .markupAmount(booking.getMarkupAmount())
                .exchangeCurrencyRate(booking.getExchangeCurrencyRate())
                .exchangeCurrency(booking.getExchangeCurrency())
                .bookingPrice(booking.getBookingPrice())
                .originalPrice(booking.getOriginalPrice())
                .buyPrice(booking.getBuyPrice())
                .profitLoss(resolveProfitLoss(booking))
                .travellerIds(travellerIds)
                .lastPaymentDate(booking.getLastPaymentDate())
                .isBookingAllowed(booking.isBookingAllowed())
                .isTicketingAllowed(booking.isTicketingAllowed())
                .taxAmount(booking.getTaxAmount())
                .brandExchangeRate(booking.getBrandExchangeRate())
                .brandCurrency(booking.getBrandCurrency())
                .bookingReference(booking.getBookingReference())
                .channel(booking.getChannel())
                .travellers(travellers)
                .TripType(booking.getTripType() != null ? booking.getTripType().name() : null)
                .travelInformation(travelInformation)
                .extras(extrasDTO)
                .business(businessSimpleDto)
                .user(userMap(booking.getCreatedBy()))
                .bundleCode(booking.getBundleCode())
                .packageBaggageList(packageBaggageDTOList)
                .airlinePnrs(convertAirlinePnrsToList(booking.getAirlinePnrs()))
                .lastPaymentDateInSeconds(booking.getLastPaymentDateInSeconds())
                .providerBookingTime(booking.getProviderBookingTime())
                .timeOffset(booking.getTimeOffset())
                .bookedTimeOffset(booking.getBookedTimeOffset())
                .sourceType(booking.getSourceType())
                .importedPnr(Boolean.TRUE.equals(booking.getImportedPnr()))
                .groupTicketType(booking.getGroupTicketType())
                .build();
    }


    private TravelInformation buildTravelInformation(com.aerionsoft.application.entity.group.TravelInformation travelInformation1) {
        TravelInformation travelInformation = new TravelInformation();
        travelInformation.setId(travelInformation1.getId());
        travelInformation.setAirlineName(travelInformation1.getAirlineName());
        travelInformation.setFlightNumber(travelInformation1.getFlightNumber());
        travelInformation.setOrigin(travelInformation1.getOrigin());
        travelInformation.setDestination(travelInformation1.getDestination());
        travelInformation.setDepartureAirport(travelInformation1.getDepartureAirport());
        travelInformation.setArrivalAirport(travelInformation1.getArrivalAirport());
        travelInformation.setDepartureDate(travelInformation1.getDepartureDate());
        travelInformation.setDepartureTime(travelInformation1.getDepartureTime());
        travelInformation.setArrivalDate(travelInformation1.getArrivalDate());
        travelInformation.setArrivalTime(travelInformation1.getArrivalTime());
        travelInformation.setFareBasis(travelInformation1.getFareBasis());
        travelInformation.setQuantity(travelInformation1.getQuantity());
        travelInformation.setCurrency(travelInformation1.getCurrency());
        travelInformation.setBaseFare(travelInformation1.getBaseFare());
        travelInformation.setEquivalentBaseFare(travelInformation1.getEquivalentBaseFare());
        travelInformation.setBaggageKg(travelInformation1.getBaggageKg());
        travelInformation.setTax(travelInformation1.getTax());
        travelInformation.setDuration(travelInformation1.getDuration());
        travelInformation.setTicketNumber(travelInformation1.getTicketNumber());
        travelInformation.setInstructions(travelInformation1.getInstructions());
        travelInformation.setFlightType(travelInformation1.getFlightType());
        travelInformation.setAirlineCode(travelInformation1.getAirlineCode());
        travelInformation.setOnewaySegmentStopCount(travelInformation1.getOnewaySegmentStopCount());
        travelInformation.setReturnSegmentStopCount(travelInformation1.getReturnSegmentStopCount());

        // Load segments with nested objects
        List<BookingSegment> segments = bookingSegmentRepository.findByTravelInformationIdOrderBySegmentOrderAsc(travelInformation1.getId());
        if (segments != null && !segments.isEmpty()) {
            List<SegmentDTO> segmentDTOs = segments.stream()
                    .map(this::mapSegmentToDto)
                    .toList();
            travelInformation.setSegments(segmentDTOs);
        }

        return travelInformation;
    }

    /**
     * Maps BookingSegment entity to SegmentDTO with nested origin, destination, and airline
     */
    private SegmentDTO mapSegmentToDto(BookingSegment segment) {
        SegmentDTO dto = SegmentDTO.builder()
                .id(segment.getId())
                .baggagePieceCount(segment.getBaggagePieceCount())
                .duration(segment.getDuration())
                .cabinClass(segment.getCabinClass())
                .noOfSeatAvailable(segment.getNoOfSeatAvailable())
                .cabinBaggage(segment.getCabinBaggage())
                .baggage(segment.getBaggage())
                .bookingCode(segment.getBookingCode())
                .segmentOrder(segment.getSegmentOrder())
                .segmentType(segment.getSegmentType())
                .build();

        segmentAirportRepository.findFirstBySegmentIdAndAirportTypeOrderByIdAsc(segment.getId(), "ORIGIN")
                .ifPresent(originAirport -> dto.setOrigin(mapAirportToDto(originAirport)));

        segmentAirportRepository.findFirstBySegmentIdAndAirportTypeOrderByIdAsc(segment.getId(), "DESTINATION")
                .ifPresent(destAirport -> dto.setDestination(mapAirportToDto(destAirport)));

        segmentAirlineRepository.findFirstBySegmentIdOrderByIdAsc(segment.getId())
                .ifPresent(airline -> dto.setAirline(mapAirlineToDto(airline)));

        return dto;
    }

    /**
     * Maps SegmentAirport entity to AirportDTO
     */
    private SegmentDTO.AirportDTO mapAirportToDto(SegmentAirport airport) {
        return SegmentDTO.AirportDTO.builder()
                .id(airport.getId())
                .airportCode(airport.getAirportCode())
                .airportName(airport.getAirportName())
                .terminal(airport.getTerminal())
                .cityCode(airport.getCityCode())
                .cityName(airport.getCityName())
                .countryCode(airport.getCountryCode())
                .countryName(airport.getCountryName())
                .time(airport.getTime())
                .build();
    }

    /**
     * Maps SegmentAirline entity to AirlineDTO
     */
    private SegmentDTO.AirlineDTO mapAirlineToDto(SegmentAirline airline) {
        return SegmentDTO.AirlineDTO.builder()
                .id(airline.getId())
                .airlineCode(airline.getAirlineCode())
                .airlineName(airline.getAirlineName())
                .flightNumber(airline.getFlightNumber())
                .fareClass(airline.getFareClass())
                .operatingCarrier(airline.getOperatingCarrier())
                .build();
    }

    private ExtrasDTO mapExtrasToDTO(Extras extras) {
        return ExtrasDTO.builder()
                .id(extras.getId())
                .seatCode(extras.getSeatCode())
                .mealCode(extras.getMealCode())
                .baggageCode(extras.getBaggageCode())
                .flightNumber(extras.getFlightNumber())
                .createdAt(extras.getCreatedAt() == null ? null : java.sql.Timestamp.valueOf(
                        timestampMapper.toRequestUserTime(extras.getCreatedAt(), null)))
                .updatedAt(extras.getUpdatedAt() == null ? null : java.sql.Timestamp.valueOf(
                        timestampMapper.toRequestUserTime(extras.getUpdatedAt(), null)))
                .build();
    }

    private List<ExtrasDTO> mapExtrasList(List<Extras> extrasList) {
        if (extrasList == null || extrasList.isEmpty()) {
            return new ArrayList<>();
        }
        return extrasList.stream()
                .map(this::mapExtrasToDTO)
                .collect(Collectors.toList());
    }

    private PackageBaggageDTO mapPackageBaggageToDTO(BookingPackageBaggage packageBaggage) {
        return PackageBaggageDTO.builder()
                .id(packageBaggage.getId())
                .pax(packageBaggage.getPax())
                .weight(packageBaggage.getWeight())
                .unit(packageBaggage.getUnit())
                .flightNumber(packageBaggage.getFlightNumber())
                .createdAt(packageBaggage.getCreatedAt() == null ? null : java.sql.Timestamp.valueOf(
                        timestampMapper.toRequestUserTime(packageBaggage.getCreatedAt(), null)))
                .updatedAt(packageBaggage.getUpdatedAt() == null ? null : java.sql.Timestamp.valueOf(
                        timestampMapper.toRequestUserTime(packageBaggage.getUpdatedAt(), null)))
                .build();
    }

    private List<PackageBaggageDTO> mapPackageBaggageList(List<BookingPackageBaggage> packageBaggageList) {
        if (packageBaggageList == null || packageBaggageList.isEmpty()) {
            return new ArrayList<>();
        }
        return packageBaggageList.stream()
                .map(this::mapPackageBaggageToDTO)
                .collect(Collectors.toList());
    }


    public BookingResponse changeStatus(Long id, BookingStatus status, String reason) {
        Booking booking = bookingRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking"));
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);
        if (reason != null && !reason.isBlank()) {
            booking.setReason(reason);
        }
        booking.setUpdatedAt(UserDateTimeUtil.now());
        booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        bookingRepo.save(booking);
        bookingTimelineService.recordSystem(booking.getId(), status, oldStatus, booking.getPnr(), booking.getTicketNo(), reason);
        auditBookingStatusChange(booking, oldStatus, status, reason, null);
        return mapToResponse(booking);
    }

    public void changeStatus(Long id, StatusChangeRequest status) {
        Booking booking = bookingRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking"));
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status.getBookingStatus());
        booking.setReason(status.getReason());
        booking.setUpdatedAt(UserDateTimeUtil.now());
        booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        // Optionally handle reason if needed
        bookingRepo.save(booking);
        bookingTimelineService.recordSystem(booking.getId(), status.getBookingStatus(), oldStatus, booking.getPnr(), booking.getTicketNo(), status.getReason());
        auditBookingStatusChange(booking, oldStatus, status.getBookingStatus(), status.getReason(), null);
        mapToResponse(booking);
    }


    public void updatePnrAndTicketNo(Long id, String pnr, String ticketNo, BookingStatus status, String reason) {
        updatePnrAndTicketNo(id, pnr, ticketNo, status, reason, null);
    }

    public void updatePnrAndTicketNo(Long id, String pnr, String ticketNo, BookingStatus status, String reason, String airlinePnrs) {
        Booking booking = bookingRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking"));
        BookingStatus oldStatus = booking.getStatus();
        booking.setPnr(pnr);
        booking.setTicketNo(ticketNo);
        booking.setUpdatedAt(UserDateTimeUtil.now());
        booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        booking.setStatus(status);
        booking.setReason(reason);
        if (airlinePnrs != null && !airlinePnrs.isEmpty()) {
            booking.setAirlinePnrs(airlinePnrs);
        }
        bookingRepo.save(booking);
        bookingTimelineService.recordSystem(booking.getId(), status, oldStatus, pnr, ticketNo, reason);
        auditBookingStatusChange(booking, oldStatus, status, reason, null);
    }


    public void updateStatusOnly(Long id, BookingStatus status) {
        Booking booking = bookingRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking"));
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);
        booking.setUpdatedAt(UserDateTimeUtil.now());
        booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        bookingRepo.save(booking);
        bookingTimelineService.recordSystem(booking.getId(), status, oldStatus, booking.getPnr(), booking.getTicketNo(), "Status updated to " + status);
        auditBookingStatusChange(booking, oldStatus, status, "Status updated to " + status, null);
    }

    public void updateBookingStatus(Long id, BookingStatus status, String reason, String ticketNo) {
        updateBookingStatus(id, status, reason, ticketNo, false);
    }

    public void updateBookingStatus(Long id, BookingStatus status, String reason, String ticketNo, boolean isAdmin) {
        updateBookingStatus(id, status, reason, ticketNo, isAdmin, null);
    }

    public void updateBookingStatus(Long id, BookingStatus status, String reason, String ticketNo, boolean isAdmin, String airlinePnrs) {
        updateBookingStatus(id, status, reason, ticketNo, isAdmin, null, airlinePnrs);
    }

    public void updateBookingStatus(Long id, BookingStatus status, String reason, String ticketNo, boolean isAdmin, Long adminUserId, String airlinePnrs) {
        Booking booking = bookingRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking"));

        BookingStatus oldStatus = booking.getStatus();

        // Check if we need to deduct wallet balance (when changing from ON_HOLD, PNR, or PROCESS to CONFIRMED/TICKETED)
        boolean shouldDeductBalance = (oldStatus == BookingStatus.ON_HOLD
                || oldStatus == BookingStatus.PNR
                || oldStatus == BookingStatus.PROCESS)
                && (status == BookingStatus.CONFIRMED || status == BookingStatus.TICKETED || status == BookingStatus.TICKET_ISSUED);

        if (shouldDeductBalance && booking.getBookingPrice() != null) {
            Long userId = booking.getCreatedBy().getId();
            Long actingUserId = booking.getActingUserId() != null ? booking.getActingUserId() : userId;

            double userExchangeRate = currencyService.getExchangeRateBasedOnUsd(
                    booking.getCreatedBy().getCurrency(), booking.getProviderName().name(), booking.getChannel());

            double bookingPrice = Double.parseDouble(booking.getBookingPrice());
            double convertedBookingPrice = bookingPrice * userExchangeRate;
            Long walletUserId = CreditLimitValidatorService.resolveWalletUserId(booking.getCreatedBy());

            boolean canOverrideBalance = hasAdminOverrideBalancePermission(adminUserId);

            userService.deductUserBalance(userId, convertedBookingPrice, booking.getProviderName().name(), canOverrideBalance,
                    "BookingService", booking.getId(), "BOOKING", actingUserId);
            log.info("➖ Deducted " + convertedBookingPrice + " from wallet for booking status update. Override used: " + canOverrideBalance);

            // Create wallet deposit record
            String depositReference = referenceGeneratorService.nextReference("FR");
            double exchangeRate = booking.getExchangeCurrencyRate() != null
                    ? Double.parseDouble(booking.getExchangeCurrencyRate()) : 1.0;

            WalletDeposit deposit = WalletDeposit.builder()
                    .userId(walletUserId)
                    .actingUserId(actingUserId)
                    .type(DepositType.PURCHASE)
                    .status(DepositStatus.APPROVED)
                    .amount(bookingPrice)
                    .exchangeRate(exchangeRate)
                    .remarks(booking.getProviderName().name().toLowerCase() + "_booking_status_update_" + booking.getPnr())
                    .reference(depositReference)
                    .transactionId(UUID.randomUUID().toString())
                    .createdAt(UserDateTimeUtil.now())
                    .exchangedAmount(convertedBookingPrice)
                    .build();

            walletDepositRepository.save(deposit);

            // Create transaction record — same converted amount as wallet deduction
            Transaction transaction = Transaction.builder()
                    .type(DepositType.PURCHASE.name())
                    .amount(convertedBookingPrice)
                    .currency(booking.getExchangeCurrency() != null ? booking.getExchangeCurrency() : "USD")
                    .exchangeRate(userExchangeRate)
                    .convertedAmount(String.valueOf(convertedBookingPrice))
                    .description("Deducted for " + booking.getProviderName().name().toLowerCase() + " booking status update from " + oldStatus + " to " + status)
                    .userId(walletUserId)
                    .createdBy("SYSTEM")
                    .createdAt(UserDateTimeUtil.now())
                    .reference(depositReference)
                    .sourceType(TransactionSourceType.BOOKING.name())
                    .sourceId(booking.getId())
                    .build();

            transactionRepository.save(transaction);
        }

        // Update booking status and ticket
        booking.setStatus(status);
        booking.setReason(reason);
        if (ticketNo != null && !ticketNo.isEmpty()) {
            booking.setTicketNo(ticketNo);

            // Parse comma-separated ticket numbers and create booking_traveller_ticket records
            String[] ticketNumbers = ticketNo.split(",");
            List<Long> travellerIds = new ArrayList<>();
            if (booking.getTravellerIds() != null && !booking.getTravellerIds().isEmpty()) {
                travellerIds = Helper.parseIds(booking.getTravellerIds());
            }

            // Create or update booking_traveller_ticket for each traveller
            for (int i = 0; i < travellerIds.size() && i < ticketNumbers.length; i++) {
                Long travellerId = travellerIds.get(i);
                String ticketNumber = ticketNumbers[i].trim();

                if (!ticketNumber.isEmpty()) {
                    try {
                        com.aerionsoft.application.entity.Booking.BookingTravellerTicket record =
                                bookingTravellerTicketRepository
                                        .findByBookingIdAndTravellerId(booking.getId(), travellerId)
                                        .orElseGet(() -> com.aerionsoft.application.entity.Booking.BookingTravellerTicket.builder()
                                                .bookingId(booking.getId())
                                                .travellerId(travellerId)
                                                .build());
                        record.setTicketNumber(ticketNumber);
                        bookingTravellerTicketRepository.save(record);
                    } catch (Exception e) {
                        // Log warning but continue with other tickets
                        System.err.println("Warning: Could not persist ticket for traveller " +
                                travellerId + ": " + e.getMessage());
                    }
                }
            }
        }
//        if (airlinePnrs != null && !airlinePnrs.isEmpty()) {
//            booking.setAirlinePnrs(airlinePnrs);
//        }
        booking.setUpdatedAt(UserDateTimeUtil.now());
        booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        bookingRepo.save(booking);
        notifyUserOnStatusChange(booking, status);
        bookingTimelineService.recordSystem(booking.getId(), status, oldStatus, booking.getPnr(), booking.getTicketNo(), reason);
        auditBookingStatusChange(booking, oldStatus, status, reason, null);
    }

    private boolean hasAdminOverrideBalancePermission(Long adminUserId) {
        if (adminUserId == null || !adminUserRepository.existsById(adminUserId)) {
            return false;
        }
        return roleAssignmentRepository.findByEntityTypeAndEntityId("ADMIN", adminUserId)
                .map(ra -> ra.getRole() != null && ra.getRole().getPermissions() != null
                        && ra.getRole().getPermissions().stream()
                        .anyMatch(p -> p.getSlug() != null && "override-balance".equalsIgnoreCase(p.getSlug())))
                .orElse(false);
    }

    private void auditBookingStatusChange(
            Booking booking,
            BookingStatus oldStatus,
            BookingStatus newStatus,
            String reason,
            Map<String, Object> extraMetadata) {
        activityBookingAuditSupport.logStatusChange(
                booking.getId(),
                booking.getPnr(),
                oldStatus,
                newStatus,
                reason,
                extraMetadata);
    }


    public void updateBookingStatus(Long id, BookingStatus status, String reason, BigDecimal toDeduction) {
        updateBookingStatus(id, status, reason, toDeduction, null);
    }

    public void updateBookingStatus(
            Long id,
            BookingStatus status,
            String reason,
            BigDecimal toDeduction,
            Map<String, Object> extraMetadata) {
        Booking booking = bookingRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking"));
        BookingStatus oldStatus = booking.getStatus();


        if (booking.getBookingPrice() != null) {
            Long userId = booking.getCreatedBy().getId();
            User user = booking.getCreatedBy();
            Long actingUserId = booking.getActingUserId() != null ? booking.getActingUserId() : userId;
            double bookingPrice = Double.parseDouble(booking.getBookingPrice());
            double finalDeduction = toDeduction != null ? toDeduction.doubleValue() : bookingPrice;
            double bookingExchangeRate = Double.parseDouble(booking.getExchangeCurrencyRate() != null ? booking.getExchangeCurrencyRate() : "1.0");

            double finalBookingPrice = (bookingPrice * bookingExchangeRate );
            if (finalBookingPrice < finalDeduction) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Deduction amount cannot be greater than booking price");
            }

//            String exchangeRate = currencyService.getExchangeRate(booking.getCreatedBy().getCurrency(), "USD", booking.getProviderName().name());
//            double exchangeRateValue = exchangeRate != null ? Double.parseDouble(exchangeRate) : 1.0;
            double refundAmount = finalBookingPrice - finalDeduction;

            // Partial refund to wallet
            userService.addUserBalance(userId, refundAmount,
                    "PARTIAL_REFUND", "BookingService", booking.getId(), "BOOKING", actingUserId);

            // Create wallet deposit record
            String depositReference = referenceGeneratorService.nextReference("FR");


            double exchangeRateAsBooking = booking.getExchangeCurrencyRate() != null ? Double.parseDouble(booking.getExchangeCurrencyRate()) : 1.0;

            WalletDeposit deposit = WalletDeposit.builder()
                    .userId(userId)
                    .actingUserId(actingUserId)
                    .type(DepositType.REFUND)
                    .status(DepositStatus.APPROVED)
                    .amount(refundAmount)
                    .exchangeRate(exchangeRateAsBooking)
                    .remarks(booking.getProviderName().name().toLowerCase() + "_booking_status_update_" + booking.getPnr())
                    .reference(depositReference)
                    .transactionId(UUID.randomUUID().toString())
                    .createdAt(UserDateTimeUtil.now())
                    .exchangedAmount(refundAmount * exchangeRateAsBooking)
                    .currency(Currency.valueOf(user.getCurrency()))
                    .build();

            WalletDeposit savedDeposit = walletDepositRepository.save(deposit);

            String transactionDesc = "Pnr: " + booking.getPnr() + ", Ticket: " + booking.getTicketNo() + ". Refunded for " + booking.getProviderName().name().toLowerCase() + " booking status update from " + oldStatus + " to " + status;

            // Create transaction record
            Transaction transaction = Transaction.builder()
                    .type(DepositType.REFUND.name())
                    .amount(refundAmount)
                    .currency(booking.getCreatedBy().getCurrency())
                    .exchangeRate(Double.valueOf(booking.getExchangeCurrencyRate()))
                    .convertedAmount(String.valueOf(refundAmount * Double.parseDouble(booking.getExchangeCurrencyRate())))
                    .description(transactionDesc)
                    .userId(userId)
                    .createdBy("SYSTEM")
                    .createdAt(UserDateTimeUtil.now())
                    .reference(depositReference)
                    .sourceType(TransactionSourceType.BOOKING.name())
                    .sourceId(booking.getId())
                    .build();

            transactionRepository.save(transaction);
        }

        if(booking.getProviderName().equals(Provider.GROUP))
        {
            int travellerCount = 0;
            if (booking.getTravellerIds() != null && !booking.getTravellerIds().isEmpty()) {
                travellerCount = Helper.parseIds(booking.getTravellerIds()).size();
            }

            GroupTicket groupTicket = groupTicketRepository.findByGdsPnr(booking.getPnr());
            if(groupTicket != null)            {
                List<PassengerFare> passengerFares = groupTicket.getPassengerFares();
                if(passengerFares != null && !passengerFares.isEmpty())                {
                    // deduct bookedQuantity based on the number of travellers being refunded/cancelled
                    for(PassengerFare pf : passengerFares)                    {
                        int newBookedQuantity = pf.getBookedQuantity() - travellerCount;
                        if (newBookedQuantity < 0) {
                            newBookedQuantity = 0;
                        }
                        pf.setBookedQuantity(newBookedQuantity);
                    }
                    groupTicketRepository.save(groupTicket);
                }
            }
        }



        // Update booking status and ticket
        booking.setStatus(status);
        booking.setReason(reason);
        booking.setUpdatedAt(UserDateTimeUtil.now());
        booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        bookingRepo.save(booking);

        // Supplier reverse is part of the same DB transaction — never swallow failures
        // (wallet credit + REFUND status must not commit if supplier books fail).
        if (extraMetadata != null && extraMetadata.get("supplierRefundCost") != null) {
            BigDecimal supplierRefundCost = toBigDecimal(extraMetadata.get("supplierRefundCost"));
            bookingSupplierInvoiceService.reverseSupplierPayableForRefundedBooking(booking, supplierRefundCost);
        } else {
            // Legacy fallback when supplierRefundCost is not supplied
            BigDecimal refundRatio = BigDecimal.ONE;
            if (booking.getBookingPrice() != null) {
                double bookingPrice = Double.parseDouble(booking.getBookingPrice());
                double exchangeRate = Double.parseDouble(
                        booking.getExchangeCurrencyRate() != null ? booking.getExchangeCurrencyRate() : "1.0");
                double finalPrice = bookingPrice * exchangeRate;
                double finalDed = toDeduction != null ? toDeduction.doubleValue() : 0.0;
                if (finalPrice > 0) {
                    double ratio = (finalPrice - finalDed) / finalPrice;
                    refundRatio = BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, ratio)));
                }
            }
            bookingSupplierInvoiceService.reverseSupplierPayableForRefundedBookingByRatio(booking, refundRatio);
        }

        bookingTimelineService.recordSystem(booking.getId(), BookingStatus.REFUND, oldStatus, booking.getPnr(), booking.getTicketNo(), reason);
        if (extraMetadata != null && extraMetadata.containsKey("refundType")) {
            activityBookingAuditSupport.logAdminRefund(
                    booking.getId(),
                    booking.getPnr(),
                    oldStatus,
                    reason,
                    extraMetadata);
        } else {
            auditBookingStatusChange(booking, oldStatus, BookingStatus.REFUND, reason, extraMetadata);
        }

        // Notifications are best-effort and must not roll back money/supplier work
        try {
            notifyUserOnStatusChange(booking, BookingStatus.REFUND);
        } catch (Exception e) {
            log.warning("Could not notify user about refunded booking "
                    + booking.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Admin-initiated refund for a booking.
     *
     * <ul>
     *   <li>FULL  – refund the entire booking price to the user's wallet.</li>
     *   <li>PARTIAL – keep {@code deductionAmount} as a penalty/fee and refund
     *       the remainder ({@code bookingPrice - deductionAmount}) to the wallet.</li>
     *   <li>{@code supplierRefundCost} – amount the supplier keeps; remaining supplier payable
     *       for the PNR equals this value. Reversal = buyPrice - supplierRefundCost.</li>
     * </ul>
     *
     * Internally delegates to the existing
     * {@link #updateBookingStatus(Long, BookingStatus, String, BigDecimal)} method
     * which handles wallet credit, transaction records, and status change.
     */
    /**
     * Admin refund is append-only: credits wallet with a new REFUND txn/deposit,
     * keeps the original PURCHASE, sets booking to REFUND, and reverses supplier payable.
     * Runs in one DB transaction — supplier reverse failure rolls back wallet + status.
     */
    @Transactional(rollbackFor = Exception.class)
    public com.aerionsoft.application.dto.booking.AdminBookingRefundResponse adminRefundBooking(
            Long bookingId,
            com.aerionsoft.application.dto.booking.AdminBookingRefundRequest request) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() == BookingStatus.REFUND) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Booking is already refunded – cannot process another admin refund");
        }

        if (booking.getBookingPrice() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Booking has no price recorded – cannot process refund");
        }

        java.math.BigDecimal bookingPrice = new java.math.BigDecimal(booking.getBookingPrice());
        double bookingExchangeRate = Double.parseDouble(booking.getExchangeCurrencyRate() != null ? booking.getExchangeCurrencyRate() : "1.0");

        double finalBookingPrice = (bookingPrice.doubleValue() * bookingExchangeRate);

        java.math.BigDecimal deduction;
        if (request.getRefundType() == com.aerionsoft.application.dto.booking.AdminBookingRefundRequest.RefundType.FULL) {
            deduction = java.math.BigDecimal.ZERO;
        } else {
            // PARTIAL
            if (request.getDeductionAmount() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "deductionAmount is required for PARTIAL refund");
            }
            if (request.getDeductionAmount().compareTo(BigDecimal.valueOf(finalBookingPrice)) > 0) {
                throw ServiceExceptions.notFound("Deduction amount (" + request.getDeductionAmount()
                        + ") cannot exceed booking price (" + finalBookingPrice + ")");
            }
            deduction = request.getDeductionAmount();
        }

        java.math.BigDecimal buyPrice = resolveBookingBuyPrice(booking);
        java.math.BigDecimal profitLoss = resolveBookingProfitLoss(booking);
        java.math.BigDecimal supplierRefundCost = request.getSupplierRefundCost();
        if (supplierRefundCost.compareTo(buyPrice) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "supplierRefundCost (" + supplierRefundCost + ") cannot exceed buy price (" + buyPrice + ")");
        }

        java.math.BigDecimal supplierPayableReversed = buyPrice.subtract(supplierRefundCost);
        java.math.BigDecimal refundedAmount = bookingPrice.subtract(deduction);
        java.math.BigDecimal netProfitLoss = computeNetProfitLossAfterRefund(profitLoss, supplierRefundCost, deduction);

        Map<String, Object> refundMetadata = new LinkedHashMap<>();
        refundMetadata.put("channel", "ADMIN_REFUND");
        refundMetadata.put("refundType", request.getRefundType().name());
        refundMetadata.put("deductionAmount", deduction);
        refundMetadata.put("refundedAmount", refundedAmount);
        refundMetadata.put("supplierRefundCost", supplierRefundCost);
        refundMetadata.put("buyPrice", buyPrice);
        refundMetadata.put("profitLoss", profitLoss);
        refundMetadata.put("netProfitLoss", netProfitLoss);
        refundMetadata.put("supplierPayableReversed", supplierPayableReversed);
        refundMetadata.put("remainingSupplierPayable", supplierRefundCost);
        refundMetadata.put("appendOnly", true);

        updateBookingStatus(bookingId, BookingStatus.REFUND, request.getReason(), deduction, refundMetadata);

        return com.aerionsoft.application.dto.booking.AdminBookingRefundResponse.builder()
                .bookingId(booking.getId())
                .pnr(booking.getPnr())
                .ticketNo(booking.getTicketNo())
                .bookingPrice(bookingPrice)
                .deductionAmount(deduction)
                .refundedAmount(refundedAmount)
                .buyPrice(buyPrice)
                .profitLoss(profitLoss)
                .netProfitLoss(netProfitLoss)
                .supplierRefundCost(supplierRefundCost)
                .supplierPayableReversed(supplierPayableReversed)
                .remainingSupplierPayable(supplierRefundCost)
                .refundType(request.getRefundType().name())
                .reason(request.getReason())
                .currency(booking.getCreatedBy() != null ? booking.getCreatedBy().getCurrency() : null)
                .build();
    }

    /**
     * Admin edit of PNR / sell / buy / pax names / agency transfer.
     * Append-only wallet and supplier adjustments; never deletes PURCHASE rows.
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminBookingEditResponse adminEditBooking(Long bookingId, AdminBookingEditRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Request is required");
        }

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() == BookingStatus.REFUND) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Booking status is REFUND – cannot edit");
        }
        if (booking.getStatus() == BookingStatus.REISSUE) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Booking status is REISSUE – cannot edit");
        }
        if (booking.getStatus() == BookingStatus.VOID) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Booking status is VOID – cannot edit");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.TICKET_CANCELLED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Booking status is CANCELLED – cannot edit");
        }

        boolean hasPnr = request.getPnr() != null && !request.getPnr().isBlank();
        boolean hasSell = request.getBookingPrice() != null;
        boolean hasBuy = request.getBuyPrice() != null;
        boolean hasTravellers = request.getTravellers() != null && !request.getTravellers().isEmpty();
        boolean hasTransfer = request.getTargetUserId() != null;
        if (!hasPnr && !hasSell && !hasBuy && !hasTravellers && !hasTransfer) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Provide at least one editable field (pnr, bookingPrice, buyPrice, travellers, targetUserId)");
        }

        Map<String, AdminBookingEditResponse.FieldChange> changes = new LinkedHashMap<>();
        List<AdminBookingEditResponse.TravellerChange> travellerChanges = new ArrayList<>();
        BigDecimal walletDeltaCharged = BigDecimal.ZERO;
        BigDecimal walletDeltaCredited = BigDecimal.ZERO;
        Long transferredFromUserId = null;
        Long transferredToUserId = null;

        User currentOwner = booking.getCreatedBy();
        if (currentOwner == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Booking has no owning agency");
        }

        User pricingUser = currentOwner;
        User transferTarget = null;
        if (hasTransfer) {
            if (request.getTargetUserId().equals(currentOwner.getId())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "targetUserId is already the booking owner");
            }
            transferTarget = userRepo.findById(request.getTargetUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getTargetUserId()));
            if (transferTarget.getParentUser() != null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "targetUserId must be a mother agency user (no parent)");
            }
            // Prices are entered in the target agency currency when transferring
            pricingUser = transferTarget;
        }

        String providerName = booking.getProviderName() != null ? booking.getProviderName().name() : "OTHERS";
        String inputCurrency = pricingUser.getCurrency() != null ? pricingUser.getCurrency() : "USD";
        double pricingRate = resolveUsdToUserRate(inputCurrency, providerName, booking.getChannel());

        // --- PNR ---
        if (hasPnr) {
            String newPnr = request.getPnr().trim();
            if (!newPnr.equalsIgnoreCase(booking.getPnr() != null ? booking.getPnr() : "")) {
                changes.put("pnr", AdminBookingEditResponse.FieldChange.builder()
                        .before(booking.getPnr())
                        .after(newPnr)
                        .build());
                booking.setPnr(newPnr);
            }
        }

        // --- Pax names (shared Traveller rows) ---
        if (hasTravellers) {
            for (AdminBookingEditRequest.TravellerNameUpdate update : request.getTravellers()) {
                if (update.getTravellerId() == null) {
                    continue;
                }
                Traveller traveller = travellerRepository.findById(update.getTravellerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Traveller", update.getTravellerId()));
                String beforeName = traveller.getFirstName() + " " + traveller.getLastName();
                if (update.getTitle() != null && !update.getTitle().isBlank()) {
                    traveller.setTitle(update.getTitle().trim());
                }
                if (update.getFirstName() != null && !update.getFirstName().isBlank()) {
                    traveller.setFirstName(update.getFirstName().trim());
                }
                if (update.getLastName() != null && !update.getLastName().isBlank()) {
                    traveller.setLastName(update.getLastName().trim());
                }
                travellerRepository.save(traveller);
                String afterName = traveller.getFirstName() + " " + traveller.getLastName();
                travellerChanges.add(AdminBookingEditResponse.TravellerChange.builder()
                        .travellerId(traveller.getId())
                        .beforeName(beforeName)
                        .afterName(afterName)
                        .build());
                changes.put("traveller:" + traveller.getId(), AdminBookingEditResponse.FieldChange.builder()
                        .before(beforeName)
                        .after(afterName)
                        .build());
            }
        }

        BigDecimal oldSellUsd = parsePriceBd(booking.getBookingPrice());
        BigDecimal oldBuyUsd = resolveBookingBuyPrice(booking);

        // --- Sell / buy prices (request amounts are in pricingUser currency → store USD) ---
        if (hasSell) {
            BigDecimal newSellUsd = toUsd(request.getBookingPrice(), pricingRate);
            String newSellStr = scaleUsd(newSellUsd);
            changes.put("bookingPrice", AdminBookingEditResponse.FieldChange.builder()
                    .before(booking.getBookingPrice())
                    .after(newSellStr)
                    .build());
            changes.put("bookingPriceInput", AdminBookingEditResponse.FieldChange.builder()
                    .before(null)
                    .after(scaleUsd(request.getBookingPrice()) + " " + inputCurrency)
                    .build());
            booking.setBookingPrice(newSellStr);
        }
        if (hasBuy) {
            BigDecimal newBuyUsd = toUsd(request.getBuyPrice(), pricingRate);
            String newBuyStr = scaleUsd(newBuyUsd);
            changes.put("buyPrice", AdminBookingEditResponse.FieldChange.builder()
                    .before(booking.getBuyPrice())
                    .after(newBuyStr)
                    .build());
            changes.put("buyPriceInput", AdminBookingEditResponse.FieldChange.builder()
                    .before(null)
                    .after(scaleUsd(request.getBuyPrice()) + " " + inputCurrency)
                    .build());
            booking.setBuyPrice(newBuyStr);
        }
        if (hasSell || hasBuy || hasTransfer) {
            // Keep booking FX aligned with the agency whose currency was used for price input
            booking.setExchangeCurrency(inputCurrency);
            booking.setExchangeCurrencyRate(String.valueOf(pricingRate));
        }
        if (hasSell || hasBuy) {
            String newPl = computeProfitLossUsd(
                    booking.getBookingPrice(), booking.getBuyPrice(), booking.getOriginalPrice());
            newPl = scaleUsd(new BigDecimal(newPl));
            changes.put("profitLoss", AdminBookingEditResponse.FieldChange.builder()
                    .before(booking.getProfitLoss())
                    .after(newPl)
                    .build());
            booking.setProfitLoss(newPl);
        }

        BigDecimal newSellUsd = parsePriceBd(booking.getBookingPrice());
        BigDecimal newBuyUsd = resolveBookingBuyPrice(booking);

        // Sell-price wallet delta on current owner (skip when transferring — handled in transfer charge)
        if (hasSell && !hasTransfer) {
            double currentOwnerRate = resolveUsdToUserRate(
                    currentOwner.getCurrency() != null ? currentOwner.getCurrency() : "USD",
                    providerName,
                    booking.getChannel());
            BigDecimal sellDeltaUsd = newSellUsd.subtract(oldSellUsd);
            if (sellDeltaUsd.compareTo(BigDecimal.ZERO) > 0) {
                double charge = sellDeltaUsd.multiply(BigDecimal.valueOf(currentOwnerRate)).doubleValue();
                applyBookingWalletDebit(booking, currentOwner, charge, sellDeltaUsd.doubleValue(),
                        "Admin sell price increase for booking " + bookingId,true);
                walletDeltaCharged = BigDecimal.valueOf(charge).setScale(2, java.math.RoundingMode.HALF_UP);
            } else if (sellDeltaUsd.compareTo(BigDecimal.ZERO) < 0) {
                double credit = sellDeltaUsd.abs().multiply(BigDecimal.valueOf(currentOwnerRate)).doubleValue();
                applyBookingWalletCredit(booking, currentOwner, credit,
                        "Admin sell price decrease for booking " + bookingId);
                walletDeltaCredited = BigDecimal.valueOf(credit).setScale(2, java.math.RoundingMode.HALF_UP);
            }
        }

        // Buy-price supplier adjust in USD (fail-closed)
        if (hasBuy && oldBuyUsd.compareTo(newBuyUsd) != 0) {
            bookingSupplierInvoiceService.adjustPayableForBuyPriceChange(booking, oldBuyUsd, newBuyUsd);
        }

        // Agency transfer (append-only wallet move)
        if (hasTransfer) {
            double purchaseCharged = sumPurchaseAmounts(bookingId);
            if (purchaseCharged > 0) {
                applyBookingWalletCredit(booking, currentOwner, purchaseCharged,
                        "Admin transfer credit (old agency) for booking " + bookingId);
            }

            double chargeNewAgency;
            if (hasSell) {
                // New sell already entered in target agency currency
                chargeNewAgency = request.getBookingPrice().doubleValue();
            } else {
                chargeNewAgency = newSellUsd.multiply(BigDecimal.valueOf(pricingRate)).doubleValue();
            }
            if (chargeNewAgency > 0) {
                applyBookingWalletDebit(booking, transferTarget, chargeNewAgency, newSellUsd.doubleValue(),
                        "Admin transfer charge (new agency) for booking " + bookingId,true);
                walletDeltaCharged = BigDecimal.valueOf(chargeNewAgency)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }
            if (purchaseCharged > 0) {
                walletDeltaCredited = BigDecimal.valueOf(purchaseCharged)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }

            transferredFromUserId = currentOwner.getId();
            transferredToUserId = transferTarget.getId();
            changes.put("createdBy", AdminBookingEditResponse.FieldChange.builder()
                    .before(currentOwner.getId())
                    .after(transferTarget.getId())
                    .build());
            booking.setCreatedBy(transferTarget);
            booking.setCreatedByName(transferTarget.getFullName() != null
                    ? transferTarget.getFullName() : transferTarget.getEmail());
            booking.setActingUserId(null);
            currentOwner = transferTarget;
        }

        booking.setUpdatedAt(UserDateTimeUtil.now());
        booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        if (request.getReason() != null) {
            booking.setReason(request.getReason());
        }
        bookingRepo.save(booking);

        Map<String, Object> auditChanges = new LinkedHashMap<>();
        changes.forEach((k, v) -> auditChanges.put(k, Map.of(
                "before", v.getBefore() != null ? v.getBefore() : "",
                "after", v.getAfter() != null ? v.getAfter() : "")));
        auditChanges.put("inputCurrency", inputCurrency);
        if (transferredFromUserId != null) {
            auditChanges.put("transfer", Map.of(
                    "from", transferredFromUserId,
                    "to", transferredToUserId));
        }
        activityBookingAuditSupport.logAdminEdit(
                booking.getId(), booking.getPnr(), request.getReason(), auditChanges);

        try {
            bookingTimelineService.recordSystem(
                    booking.getId(),
                    booking.getStatus(),
                    booking.getStatus(),
                    booking.getPnr(),
                    booking.getTicketNo(),
                    "Admin edit: " + request.getReason());
        } catch (Exception e) {
            log.warning("Could not record timeline for admin edit on booking "
                    + bookingId + ": " + e.getMessage());
        }

        return AdminBookingEditResponse.builder()
                .bookingId(booking.getId())
                .pnr(booking.getPnr())
                .ticketNo(booking.getTicketNo())
                .bookingPrice(parsePriceBd(booking.getBookingPrice()))
                .buyPrice(resolveBookingBuyPrice(booking))
                .profitLoss(parsePriceBd(booking.getProfitLoss()))
                .inputCurrency(inputCurrency)
                .bookingPriceInput(hasSell ? request.getBookingPrice() : null)
                .buyPriceInput(hasBuy ? request.getBuyPrice() : null)
                .ownerUserId(currentOwner.getId())
                .ownerName(booking.getCreatedByName())
                .reason(request.getReason())
                .changes(changes)
                .walletDeltaCharged(walletDeltaCharged)
                .walletDeltaCredited(walletDeltaCredited)
                .transferredFromUserId(transferredFromUserId)
                .transferredToUserId(transferredToUserId)
                .travellerChanges(travellerChanges)
                .build();
    }

    private double resolveUsdToUserRate(String userCurrency, String providerName, String channel) {
        if (userCurrency == null || userCurrency.isBlank() || "USD".equalsIgnoreCase(userCurrency)) {
            return 1.0;
        }
        try {
            double rate = currencyService.getExchangeRateBasedOnUsd(userCurrency, providerName, channel);
            if (rate <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "Invalid exchange rate for currency " + userCurrency);
            }
            return rate;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Currency conversion rate not found for " + userCurrency + ": " + e.getMessage());
        }
    }

    /** Convert amount in user currency to USD using USD→user rate. */
    private static BigDecimal toUsd(BigDecimal amountInUserCurrency, double usdToUserRate) {
        if (amountInUserCurrency == null) {
            return BigDecimal.ZERO;
        }
        if (usdToUserRate <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Exchange rate must be greater than zero");
        }
        return amountInUserCurrency
                .divide(BigDecimal.valueOf(usdToUserRate), 6, java.math.RoundingMode.HALF_UP)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private double sumPurchaseAmounts(Long bookingId) {
        return transactionRepository.findBySourceTypeAndSourceId(
                        TransactionSourceType.BOOKING.name(), bookingId)
                .stream()
                .filter(t -> t.getType() != null && "PURCHASE".equalsIgnoreCase(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0)
                .sum();
    }

    private void applyBookingWalletDebit(
            Booking booking, User owner, double convertedAmount, double usdAmount, String description,
            boolean canOverrideBalance) {
        applyBookingWalletDebit(booking, owner, convertedAmount, usdAmount, description, canOverrideBalance, null);
    }

    private void applyBookingWalletDebit(Booking booking, User owner, double convertedAmount, double usdAmount, String description,
                                         boolean canOverrideBalance, String remarks) {
        Long actingUserId = booking.getActingUserId() != null ? booking.getActingUserId() : owner.getId();
        Long walletUserId = CreditLimitValidatorService.resolveWalletUserId(owner);
        String provider = booking.getProviderName() != null ? booking.getProviderName().name() : "OTHERS";

        userService.deductUserBalance(owner.getId(), convertedAmount, provider, canOverrideBalance,
                "BookingService", booking.getId(), "BOOKING", actingUserId);

        String depositReference = referenceGeneratorService.nextReference("FR");
        double exchangeRate = parseExchangeRate(booking);
        String depositRemarks = remarks != null && !remarks.isBlank()
                ? remarks
                : "admin_edit_" + booking.getPnr();
        WalletDeposit deposit = WalletDeposit.builder()
                .userId(walletUserId)
                .actingUserId(actingUserId)
                .type(DepositType.PURCHASE)
                .status(DepositStatus.APPROVED)
                .amount(usdAmount)
                .exchangeRate(exchangeRate)
                .remarks("admin_edit_" + booking.getPnr())
                .reference(depositReference)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(UserDateTimeUtil.now())
                .exchangedAmount(convertedAmount)
                .currency(owner.getCurrency() != null ? Currency.valueOf(owner.getCurrency()) : Currency.USD)
                .build();
        walletDepositRepository.save(deposit);

        Transaction transaction = Transaction.builder()
                .type(DepositType.PURCHASE.name())
                .amount(convertedAmount)
                .currency(owner.getCurrency() != null ? owner.getCurrency() : "USD")
                .exchangeRate(exchangeRate)
                .convertedAmount(String.valueOf(convertedAmount))
                .description(description)
                .userId(walletUserId)
                .createdBy("ADMIN")
                .createdAt(UserDateTimeUtil.now())
                .reference(depositReference)
                .sourceType(TransactionSourceType.BOOKING.name())
                .sourceId(booking.getId())
                .build();
        transactionRepository.save(transaction);
    }

    private void applyBookingWalletCredit(Booking booking, User owner, double amount, String description) {
        Long actingUserId = booking.getActingUserId() != null ? booking.getActingUserId() : owner.getId();
        Long walletUserId = CreditLimitValidatorService.resolveWalletUserId(owner);

        userService.addUserBalance(owner.getId(), amount,
                "ADMIN_EDIT_CREDIT", "BookingService", booking.getId(), "BOOKING", actingUserId);

        String depositReference = referenceGeneratorService.nextReference("FR");
        double exchangeRate = parseExchangeRate(booking);
        WalletDeposit deposit = WalletDeposit.builder()
                .userId(walletUserId)
                .actingUserId(actingUserId)
                .type(DepositType.REFUND)
                .status(DepositStatus.APPROVED)
                .amount(amount)
                .exchangeRate(exchangeRate)
                .remarks("admin_edit_" + booking.getPnr())
                .reference(depositReference)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(UserDateTimeUtil.now())
                .exchangedAmount(amount)
                .currency(owner.getCurrency() != null ? Currency.valueOf(owner.getCurrency()) : Currency.USD)
                .build();
        walletDepositRepository.save(deposit);

        Transaction transaction = Transaction.builder()
                .type(DepositType.REFUND.name())
                .amount(amount)
                .currency(owner.getCurrency() != null ? owner.getCurrency() : "USD")
                .exchangeRate(exchangeRate)
                .convertedAmount(String.valueOf(amount))
                .description(description)
                .userId(walletUserId)
                .createdBy("ADMIN")
                .createdAt(UserDateTimeUtil.now())
                .reference(depositReference)
                .sourceType(TransactionSourceType.BOOKING.name())
                .sourceId(booking.getId())
                .build();
        transactionRepository.save(transaction);
    }

    private static BigDecimal parsePriceBd(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim()).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String scaleUsd(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static double parseExchangeRate(Booking booking) {
        if (booking.getExchangeCurrencyRate() == null || booking.getExchangeCurrencyRate().isBlank()) {
            return 1.0;
        }
        try {
            return Double.parseDouble(booking.getExchangeCurrencyRate());
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    public BigDecimal resolveBookingBuyPrice(Booking booking) {
        if (booking.getBuyPrice() != null && !booking.getBuyPrice().isBlank()) {
            return new BigDecimal(booking.getBuyPrice());
        }
        if (booking.getOriginalPrice() != null && !booking.getOriginalPrice().isBlank()) {
            return new BigDecimal(booking.getOriginalPrice());
        }
        if (booking.getBookingPrice() != null && !booking.getBookingPrice().isBlank()) {
            return new BigDecimal(booking.getBookingPrice());
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal resolveBookingProfitLoss(Booking booking) {
        return new BigDecimal(resolveProfitLoss(booking));
    }

    /**
     * Net margin after refund: original profitLoss minus supplier cost kept, plus any customer fee retained.
     */
    public BigDecimal computeNetProfitLossAfterRefund(
            BigDecimal profitLoss,
            BigDecimal supplierRefundCost,
            BigDecimal customerRetention) {
        BigDecimal supplierCost = supplierRefundCost != null ? supplierRefundCost : BigDecimal.ZERO;
        BigDecimal retention = customerRetention != null ? customerRetention : BigDecimal.ZERO;
        return profitLoss.subtract(supplierCost).add(retention);
    }
    /**
     * Net margin after reissue: original profitLoss plus reissue agency margin (charge - supplier cost).
     */
    public BigDecimal computeNetProfitLossAfterReissue(
            BigDecimal profitLoss,
            BigDecimal supplierReissueCost,
            BigDecimal reissueChargeAmount) {
        BigDecimal supplierCost = supplierReissueCost != null ? supplierReissueCost : BigDecimal.ZERO;
        BigDecimal charge = reissueChargeAmount != null ? reissueChargeAmount : BigDecimal.ZERO;
        return profitLoss.add(charge.subtract(supplierCost));
    }

    /**
     * Updates segment departure/arrival times on an existing booking (used on REISSUE finalize).
     * Matches segments by {@code segmentOrder} and refreshes travel_information summary dates.
     */
    public void updateBookingSegmentDates(Booking booking, List<ReissueSegmentDateUpdate> segmentUpdates) {
        if (segmentUpdates == null || segmentUpdates.isEmpty()) {
            return;
        }

        com.aerionsoft.application.entity.group.TravelInformation travelInfo =
                travelInformationRepository.findByBookingId(booking.getId());
        if (travelInfo == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Booking has no travel information — cannot update segment dates");
        }

        List<BookingSegment> existingSegments =
                bookingSegmentRepository.findByTravelInformationIdOrderBySegmentOrderAsc(travelInfo.getId());
        if (existingSegments == null || existingSegments.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Booking has no segments — cannot update segment dates");
        }

        Map<Integer, BookingSegment> segmentsByOrder = existingSegments.stream()
                .filter(s -> s.getSegmentOrder() != null)
                .collect(Collectors.toMap(BookingSegment::getSegmentOrder, s -> s, (a, b) -> a));

        for (ReissueSegmentDateUpdate update : segmentUpdates) {
            if (update.getSegmentOrder() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "segmentOrder is required for each segment date update");
            }
            boolean hasDepTime = update.getDepTime() != null && !update.getDepTime().isBlank();
            boolean hasArrTime = update.getArrTime() != null && !update.getArrTime().isBlank();
            if (!hasDepTime && !hasArrTime) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "Each segment update must include depTime and/or arrTime (segmentOrder="
                                + update.getSegmentOrder() + ")");
            }

            BookingSegment segment = segmentsByOrder.get(update.getSegmentOrder());
            if (segment == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "No segment with segmentOrder " + update.getSegmentOrder() + " on this booking");
            }

            if (hasDepTime) {
                SegmentAirport origin = segmentAirportRepository
                        .findFirstBySegmentIdAndAirportTypeOrderByIdAsc(segment.getId(), "ORIGIN")
                        .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR,
                                "Segment " + update.getSegmentOrder() + " has no origin airport record"));
                origin.setTime(update.getDepTime().trim());
                segmentAirportRepository.save(origin);
            }

            if (hasArrTime) {
                SegmentAirport destination = segmentAirportRepository
                        .findFirstBySegmentIdAndAirportTypeOrderByIdAsc(segment.getId(), "DESTINATION")
                        .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR,
                                "Segment " + update.getSegmentOrder() + " has no destination airport record"));
                destination.setTime(update.getArrTime().trim());
                segmentAirportRepository.save(destination);
            }
        }

        refreshTravelInformationDatesFromSegments(travelInfo, existingSegments);
        travelInformationRepository.save(travelInfo);
    }

    private void refreshTravelInformationDatesFromSegments(
            com.aerionsoft.application.entity.group.TravelInformation travelInfo,
            List<BookingSegment> segments) {
        BookingSegment firstSegment = segments.get(0);
        BookingSegment lastSegment = segments.get(segments.size() - 1);

        segmentAirportRepository.findFirstBySegmentIdAndAirportTypeOrderByIdAsc(firstSegment.getId(), "ORIGIN")
                .ifPresent(origin -> {
                    travelInfo.setDepartureTime(origin.getTime());
                    travelInfo.setDepartureDate(extractIsoDatePart(origin.getTime()));
                });

        segmentAirportRepository.findFirstBySegmentIdAndAirportTypeOrderByIdAsc(lastSegment.getId(), "DESTINATION")
                .ifPresent(destination -> {
                    travelInfo.setArrivalTime(destination.getTime());
                    travelInfo.setArrivalDate(extractIsoDatePart(destination.getTime()));
                });
    }

    private String extractIsoDatePart(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        if (dateTime.contains("T")) {
            return dateTime.split("T")[0];
        }
        return dateTime.length() >= 10 ? dateTime.substring(0, 10) : dateTime;
    }

    /**
     * Complete a REISSUE ticket action: charge agency wallet, add supplier payable, set booking to REISSUE.
     * Runs in one DB transaction — supplier failure rolls back wallet debit and status change.
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeTicketActionReissue(
            Booking booking,
            BigDecimal chargeAmountUsd,
            BigDecimal supplierReissueCostUsd,
            java.time.LocalDate reissueDate,
            Long ticketActionRequestId,
            String reason,
            List<ReissueSegmentDateUpdate> segmentUpdates) {
        if (chargeAmountUsd == null || chargeAmountUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Reissue charge amount must be greater than zero");
        }
        if (supplierReissueCostUsd == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "supplierRefundCost is required when completing a reissue");
        }
        if (supplierReissueCostUsd.compareTo(chargeAmountUsd) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "supplierRefundCost (" + supplierReissueCostUsd + ") cannot exceed reissue charge ("
                            + chargeAmountUsd + ")");
        }

        User owner = booking.getCreatedBy();
        if (owner == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Booking has no owning agency");
        }

        double exchangeRate = parseExchangeRate(booking);
        double chargeConverted = chargeAmountUsd
                .multiply(BigDecimal.valueOf(exchangeRate))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();

        applyBookingWalletDebit(
                booking,
                owner,
                chargeConverted,
                chargeAmountUsd.doubleValue(),
                "Ticket action reissue charge for booking " + booking.getId()
                        + " (reissue date " + (reissueDate != null ? reissueDate : "—") + ")",
                true,
                "ticket_action_reissue_" + booking.getPnr());

        bookingSupplierInvoiceService.recordReissueCharge(
                booking, supplierReissueCostUsd, reissueDate, ticketActionRequestId);

        if (segmentUpdates != null && !segmentUpdates.isEmpty()) {
            updateBookingSegmentDates(booking, segmentUpdates);
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.REISSUE);
        booking.setReason(reason);
        booking.setUpdatedAt(UserDateTimeUtil.now());
        booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
        bookingRepo.save(booking);

        bookingTimelineService.recordSystem(
                booking.getId(), BookingStatus.REISSUE, oldStatus, booking.getPnr(), booking.getTicketNo(), reason);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "TICKET_ACTION");
        metadata.put("ticketActionRequestId", ticketActionRequestId);
        metadata.put("ticketActionType", "REISSUE");
        metadata.put("reissueDate", reissueDate != null ? reissueDate.toString() : null);
        metadata.put("reissueChargeAmountUsd", chargeAmountUsd);
        metadata.put("supplierReissueCost", supplierReissueCostUsd);
        metadata.put("balanceCheckBypassed", true);
        if (segmentUpdates != null && !segmentUpdates.isEmpty()) {
            metadata.put("segmentsUpdated", true);
            metadata.put("segmentDateUpdates", segmentUpdates);
        }
        auditBookingStatusChange(booking, oldStatus, BookingStatus.REISSUE, reason, metadata);

        try {
            notifyUserOnStatusChange(booking, BookingStatus.REISSUE);
        } catch (Exception e) {
            log.warning("Could not notify user about reissued booking "
                    + booking.getId() + ": " + e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    public List<BookingTimelineDTO> getBookingTimeline(Long bookingId) {
        // Ensure booking exists
        bookingRepo.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return bookingTimelineService.getTimeline(bookingId);
    }

    public List<BookingTimelineDTO> getFlightActivityBySession(String sessionId) {
        return bookingTimelineService.getTimelineBySession(sessionId);
    }

    public List<BookingTimelineDTO> getFlightActivityByUser(Long userId, int limit) {
        return bookingTimelineService.getTimelineByUser(userId, limit);
    }

    private LastTenBookings mapBookDto(Booking booking) {
        LastTenBookings dto = new LastTenBookings();
        dto.setCreatedAt(timestampMapper.toRequestUserTimeString(
                booking.getCreatedAt(),
                booking.getCreatedTimeOffset() != null ? booking.getCreatedTimeOffset() : booking.getTimeOffset()));
        dto.setBookingId(booking.getId());
        dto.setStatus(booking.getStatus().toString());
        dto.setTraveller(mapTravellerShortDto(booking.getTraveller()));
        dto.setCreatedBy(userMap(booking.getCreatedBy()));
        dto.setAgencyUser(userMap(booking.getCreatedBy().getParentUser()));

        return dto;
    }


    private TravellerShortDto mapTravellerShortDto(Traveller traveller) {
        TravellerShortDto dto = new TravellerShortDto();

        dto.setId(traveller.getId());
        dto.setFullName(traveller.getFirstName() + " " + traveller.getLastName());

        return dto;
    }


    public Booking bookingByPnr(String pnr) {
        return bookingRepo.findByPnr(pnr)
                .orElseThrow(() -> ServiceExceptions.notFound("Booking not found for confirmationId/pnr: " + pnr));
    }

    private void notifyUserOnStatusChange(Booking booking, BookingStatus status) {
        try {
            Long userId = booking.getCreatedBy().getId();
            String userEmail = booking.getCreatedBy().getEmail();

            if (status == BookingStatus.CONFIRMED) {
                notificationHelper.sendBookingConfirmation(
                        userId,
                        userEmail,
                        booking.getBookingReference(),
                        booking.getBookingPrice(),
                        booking.getExchangeCurrency() != null ? booking.getExchangeCurrency() : "USD",
                        booking.getId(),
                        true  // Send email
                );
                // Notify all admins about the confirmed booking
                notifyAdminsAboutBooking(booking, status);
            } else if (status == BookingStatus.PNR) {
                // Notify all admins when booking has PNR status
                notifyAdminsAboutBooking(booking, status);
            } else if (status == BookingStatus.TICKETED || status == BookingStatus.TICKET_ISSUED) {
                notificationHelper.sendTicketIssued(
                        userId,
                        userEmail,
                        booking.getBookingReference(),
                        booking.getPnr(),
                        booking.getId(),
                        true  // Send email
                );
            } else if (status == BookingStatus.CANCELLED || status == BookingStatus.TICKET_CANCELLED || status == BookingStatus.VOID || status == BookingStatus.REJECTED) {
                notificationHelper.sendBookingCancellation(
                        userId,
                        userEmail,
                        booking.getBookingReference(),
                        booking.getId(),
                        true  // Send email
                );
            } else if (status == BookingStatus.REFUND) {
                notificationHelper.sendCustomNotification(
                        userId,
                        com.aerionsoft.application.enums.notification.NotificationType.BOOKING_CANCELLED,
                        com.aerionsoft.application.enums.notification.NotificationPriority.HIGH,
                        "Booking Refunded",
                        "Your booking " + booking.getBookingReference() + " has been refunded.",
                        "/bookings/" + booking.getId(),
                        "View Booking"
                );
            } else if (status == BookingStatus.REISSUE) {
                notificationHelper.sendCustomNotification(
                        userId,
                        NotificationType.GENERAL,
                        NotificationPriority.HIGH,
                        "Ticket Reissued",
                        "Your booking " + booking.getBookingReference() + " has been reissued.",
                        "/bookings/" + booking.getId(),
                        "View Booking"
                );
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Notify all admin users about a new booking with CONFIRMED or PNR status
     */
    public void notifyAdminsAboutHoldToBookFailure(Booking booking, String errorMessage) {
        try {
            List<AdminUser> admins = adminUserRepository.findAdminsByRoleSlug("admin");
            if (admins == null || admins.isEmpty()) {
                return;
            }

            String userName = booking.getCreatedBy() != null ?
                    (booking.getCreatedBy().getFullName() != null ?
                            booking.getCreatedBy().getFullName() :
                            booking.getCreatedBy().getEmail()) :
                    "Unknown User";
            String pnr = booking.getPnr() != null ? booking.getPnr() : "N/A";
            String title = "Hold-to-Book Failed";
            String message = String.format(
                    "Hold-to-Book failed for booking by %s. Ref: %s, PNR: %s. Error: %s",
                    userName,
                    booking.getBookingReference(),
                    pnr,
                    errorMessage != null ? errorMessage : "Unknown error"
            );
            String actionUrl = "/bookings/" + booking.getId();

            for (AdminUser admin : admins) {
                if (admin != null && admin.getId() != null) {
                    try {
                        notificationHelper.sendCustomNotification(
                                admin.getId(),
                                com.aerionsoft.application.enums.notification.NotificationType.SYSTEM_ALERT,
                                com.aerionsoft.application.enums.notification.NotificationPriority.HIGH,
                                title,
                                message,
                                actionUrl,
                                "View Booking"
                        );
                    } catch (Exception ignored) {
                        // continue notifying other admins
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail - don't break booking process
        }
    }

    private void notifyAdminsAboutBooking(Booking booking, BookingStatus status) {
        try {
            // Get all admins with ADMIN role
            List<AdminUser> admins = adminUserRepository.findAdminsByRoleSlug("admin");

            if (admins == null || admins.isEmpty()) {
                return;
            }

            String userName = booking.getCreatedBy() != null ?
                    (booking.getCreatedBy().getFullName() != null ?
                            booking.getCreatedBy().getFullName() :
                            booking.getCreatedBy().getEmail()) :
                    "Unknown User";
            String currency = booking.getExchangeCurrency() != null ? booking.getExchangeCurrency() : "USD";
            String pnr = booking.getPnr() != null ? booking.getPnr() : "N/A";

            String title, message, actionUrl;

            if (status == BookingStatus.CONFIRMED) {
                title = "New Confirmed Booking";
                message = String.format("New booking confirmed by %s. Booking Ref: %s, PNR: %s, Amount: %s %s",
                        userName,
                        booking.getBookingReference(),
                        pnr,
                        booking.getBookingPrice(),
                        currency);
                actionUrl = "/bookings/" + booking.getId();
            } else if (status == BookingStatus.PNR) {
                title = "New PNR Booking";
                message = String.format("New PNR booking by %s. Booking Ref: %s, PNR: %s, Amount: %s %s",
                        userName,
                        booking.getBookingReference(),
                        pnr,
                        booking.getBookingPrice(),
                        currency);
                actionUrl = "/bookings/" + booking.getId();
            } else {
                return; // Only notify for CONFIRMED and PNR statuses
            }

            for (AdminUser admin : admins) {
                if (admin != null && admin.getId() != null) {
                    try {
                        notificationHelper.sendCustomNotification(
                                admin.getId(),
                                com.aerionsoft.application.enums.notification.NotificationType.BOOKING_CONFIRMED,
                                com.aerionsoft.application.enums.notification.NotificationPriority.HIGH,
                                title,
                                message,
                                actionUrl,
                                "View Booking"
                        );
                    } catch (Exception e) {
                        // Log error but continue notifying other admins
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail - don't break booking process
        }
    }

    /**
     * Find bookings and travelers by GF code
     *
     * @param gfCode The group flight code
     * @return PnrTravelersResponse with booking and traveler details
     */
    public PnrTravelersResponse findBookingsByGfCode(String gfCode) {
        // Find GroupTicket by GF code
        GroupTicket groupTicket = groupTicketRepository.findByGfCode(gfCode);
        if (groupTicket == null) {
            throw ServiceExceptions.notFound("Group ticket not found for GF code: " + gfCode);
        }

        String pnr = groupTicket.getGdsPnr();

        // Find all bookings with this PNR
        List<Booking> bookings = bookingRepo.findByPnrContainingIgnoreCase(pnr);

        if (bookings == null || bookings.isEmpty()) {
            throw ServiceExceptions.notFound("No bookings found for PNR: " + pnr);
        }

        // Build response
        List<PnrTravelersResponse.BookingWithTravelers> bookingWithTravelersList = new ArrayList<>();

        for (Booking booking : bookings) {
            // Get travelers for this booking
            List<TravellerResponse> travelers = new ArrayList<>();
            Long motherUserId = booking.getCreatedBy().getId();
            Optional<BusinessEntity> optionalBusinessEntity = Optional.of(businessRepository
                    .findByMotherUserId(motherUserId).orElseThrow(
                            () -> new ResourceNotFoundException("Business mother user id not found")
                    )
            );

            BusinessEntity businessEntity = optionalBusinessEntity.get();

            if (booking.getTravellerIds() != null && !booking.getTravellerIds().isEmpty()) {
                String[] travellerIdArray = booking.getTravellerIds().split(",");
                for (String travellerIdStr : travellerIdArray) {
                    try {
                        Long travellerId = Long.parseLong(travellerIdStr.trim());
                        TravellerResponse traveller = travellerService.getTravellerById(travellerId);
                        if (traveller != null) {
                            travelers.add(traveller);
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid IDs
                    } catch (RuntimeException e) {
                        // Skip if traveller not found
                    }
                }
            }

            // Get segments for this booking
            List<SegmentDTO> segments = new ArrayList<>();
            com.aerionsoft.application.entity.group.TravelInformation travelInfo =
                    travelInformationRepository.findByBookingId(booking.getId());

            if (travelInfo != null) {
                List<BookingSegment> bookingSegments =
                        bookingSegmentRepository.findByTravelInformationIdOrderBySegmentOrderAsc(travelInfo.getId());

                if (bookingSegments != null && !bookingSegments.isEmpty()) {
                    segments = bookingSegments.stream()
                            .map(this::mapSegmentToDto)
                            .toList();
                }
            }

            PnrTravelersResponse.BookingWithTravelers bookingWithTravelers = PnrTravelersResponse.BookingWithTravelers.builder()
                    .bookingId(booking.getId())
                    .bookingReference(booking.getBookingReference())
                    .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                    .bookingDate(booking.getBookingDate() != null ? booking.getBookingDate().toString() : null)
                    .travelers(travelers)
                    .segments(segments)
                    .agencyInfo(new AgencyInfo(businessEntity.getId(),motherUserId,businessEntity.getCompanyName(),
                            businessEntity.getCompanyEmail(),businessEntity.getCompanyPhone(),
                            businessEntity.getCompanyAddress()))
                    .build();

            bookingWithTravelersList.add(bookingWithTravelers);
        }

        return PnrTravelersResponse.builder()
                .gfCode(gfCode)
                .groupTicket(toDTO(groupTicket))
                .pnr(groupTicket.getGdsPnr())
                .airlinePnr(groupTicket.getAirlinePnr())
                .origin(groupTicket.getOrigin())
                .destination(groupTicket.getDestination())
                .airlineCode(groupTicket.getAirlineCode())
                .airlineName(groupTicket.getAirlineName())
                .bookings(bookingWithTravelersList)
                .build();
    }

    private GroupTicketDTO toDTO(GroupTicket entity) {
        GroupTicketDTO dto = new GroupTicketDTO();
        dto.setGfCode(entity.getGfCode());
        dto.setTitle(entity.getTitle());
        dto.setType(entity.getType());
        if (entity.getTicketType() != null && !entity.getTicketType().isBlank()) {
            dto.setTicketType(GroupTicketType.fromValue(entity.getTicketType()));
        }
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setSpecialInstructions(entity.getSpecialInstructions());
        dto.setAirlineCode(entity.getAirlineCode());
        dto.setAirlineName(entity.getAirlineName());
        dto.setVendorName(entity.getVendorName());
        dto.setBookingStarts(entity.getBookingStarts());
        dto.setBookingEnds(entity.getBookingEnds());
        dto.setOrigin(entity.getOrigin());
        dto.setDestination(entity.getDestination());
        dto.setFareCurrency(entity.getFareCurrency());
        dto.setGdsPnr(entity.getGdsPnr());
        dto.setAirlinePnr(entity.getAirlinePnr());
        dto.setArrivalTime(entity.getArrivalTime());
        dto.setDepartureTime(entity.getDepartureTime());
        dto.setDepartureDate(entity.getDepartureDate());
        dto.setArrivalDate(entity.getArrivalDate());
        dto.setFlightType(entity.getFlightType());
        dto.setCosting(entity.getCosting());
        dto.setSaleStatus(entity.getSaleStatus());
        if (entity.getSupplier() != null) {
            dto.setSupplierId(entity.getSupplier().getId());
            dto.setSupplierName(entity.getSupplier().getName());
        }
        return dto;
    }

    public boolean existsByPnr(String pnr) {
        return pnr != null && !pnr.isBlank() && bookingRepo.existsByPnrIgnoreCase(pnr.trim());
    }

    public boolean manualStatusChange(Long bookingId, BookingStatus newStatus, String reason) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Allow manual status change only if current status is ON_HOLD or PNR
        if (booking.getStatus() == BookingStatus.PNR) {
            BookingStatus oldStatus = booking.getStatus();
            booking.setStatus(newStatus);
            booking.setReason(reason);
            booking.setUpdatedAt(UserDateTimeUtil.now());
            booking.setUpdatedTimeOffset(UserDateTimeUtil.currentOffset());
            bookingRepo.save(booking);
            auditBookingStatusChange(booking, oldStatus, newStatus, reason, null);
            return true;
        }
        return false;
    }
}
