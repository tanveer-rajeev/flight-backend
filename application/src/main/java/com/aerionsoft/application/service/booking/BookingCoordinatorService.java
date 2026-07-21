package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.dto.booking.*;
import com.aerionsoft.application.dto.booking.core.*;
import com.aerionsoft.application.dto.booking.core.Record;
import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.entity.client.*;
import com.aerionsoft.application.entity.group.*;
import com.aerionsoft.application.entity.group.TravelInformation;
import com.aerionsoft.application.repository.client.*;
import com.aerionsoft.application.util.UserDateTimeUtil;
import com.aerionsoft.application.service.client.SupplierResolverService;
import com.aerionsoft.application.service.webhook.WebhookAlertDispatchService;
import com.aerionsoft.application.service.common.PlatformProviderService;
import com.aerionsoft.application.service.wallet.CreditLimitValidatorService;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.service.errorlog.ErrorLogService;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.service.wallet.ReferenceGeneratorService;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.flight.search.extras.FinalFare;
import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.dto.traveller.TravellerResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.Booking.BookingTravellerTicket;
import com.aerionsoft.application.entity.group.SegmentAirline;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.booking.BookType;
import com.aerionsoft.application.enums.booking.BookingClass;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.BookingType;
import com.aerionsoft.application.enums.booking.PassenserEnum;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.enums.booking.TripType;
import com.aerionsoft.application.enums.client.InvoiceStatus;
import com.aerionsoft.application.enums.client.InvoiceType;
import com.aerionsoft.application.enums.common.MicroserviceType;
import com.aerionsoft.application.enums.common.UsingPortal;
import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.enums.user.Gender;
import com.aerionsoft.application.enums.wallet.DepositStatus;
import com.aerionsoft.application.enums.wallet.DepositType;
import com.aerionsoft.application.enums.wallet.TransactionSourceType;
import com.aerionsoft.application.enums.common.Currency;
import com.aerionsoft.application.exception.MicroserviceException;
import com.aerionsoft.application.interafces.BookingInterface;
import com.aerionsoft.application.interafces.UserInterface;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.repository.booking.BookingSegmentRepository;
import com.aerionsoft.application.repository.booking.BookingTravellerTicketRepository;
import com.aerionsoft.application.repository.booking.SegmentAirlineRepository;
import com.aerionsoft.application.repository.booking.SegmentAirportRepository;
import com.aerionsoft.application.repository.booking.TravelInformationRepository;
import com.aerionsoft.application.repository.finance.AccountHeadRepository;
import com.aerionsoft.application.repository.access.RoleAssignmentRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.admin.GroupTicketService;
import com.aerionsoft.application.util.Helper;
import com.aerionsoft.application.util.TransactionAuditHelper;
import jakarta.transaction.Transactional;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.flight.GetReservationRequest;
import com.aerionsoft.application.dto.flight.GetReservationResponse;
import com.aerionsoft.application.dto.flight.LoadBookingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Period;
import java.util.*;
import java.util.logging.Logger;


@Service
public class BookingCoordinatorService {

    private final UserInterface userService;
    private final BookingInterface bookingService;
    private final TravellerService travellerService;
    private final BookingPriceService bookingPriceService;
    private final WalletDepositRepository walletDepositRepository;
    private final TravelInformationRepository travelInformationRepository;
    private final BookingSegmentRepository bookingSegmentRepository;
    private final SegmentAirportRepository segmentAirportRepository;
    private final SegmentAirlineRepository segmentAirlineRepository;
    private final TransactionRepository transactionRepository;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final GroupTicketService groupTicketService;
    private final CurrencyService currencyService;
    private final ErrorLogService errorLogService;
    private final TicketingDeadlineService ticketingDeadlineService;
    private final BusinessService businessService;
    private final BookingTravellerTicketRepository bookingTravellerTicketRepository;
    private final BookingService bookingReadService;
    private final CreditLimitValidatorService creditLimitValidatorService;
    private final SupplierRepository supplierRepository;
    private final InvoiceLedgerRepository invoiceLedgerRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final AccountHeadRepository accountHeadRepository;
    private final SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;
    private final String coreBookingBaseUrl;
    private final String coreBookingEndpoint;
    private final String coreIssueEndpoint;
    private final String coreHoldToBookEndpoint;
    private final String coreRepricingEndpoint;
    private final String coreCancelBookingEndpoint;
    private final String salt;
    private final String flightApiUrl;
    private final PlatformProviderService platformProviderService;
    private final SupplierResolverService supplierResolverService;
    private final WebhookAlertDispatchService webhookAlertDispatchService;
    private final HoldToBookFailurePublisher holdToBookFailurePublisher;
    private final BookingRepository bookingRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final CoreBookingApiLogService coreBookingApiLogService;
    private final BookingCreationDuplicateGuardService bookingCreationDuplicateGuardService;
    Logger log = Logger.getLogger(BookingCoordinatorService.class.getName());

    public BookingCoordinatorService(
            UserInterface userService,
            BookingInterface bookingService,
            TravellerService travellerService,
            BookingPriceService bookingPriceService,
            WalletDepositRepository walletDepositRepository,
            TravelInformationRepository travelInformationRepository,
            BookingSegmentRepository bookingSegmentRepository,
            SegmentAirportRepository segmentAirportRepository,
            SegmentAirlineRepository segmentAirlineRepository,
            TransactionRepository transactionRepository,
            ReferenceGeneratorService referenceGeneratorService,
            GroupTicketService groupTicketService,
            CurrencyService currencyService,
            ErrorLogService errorLogService,
            TicketingDeadlineService ticketingDeadlineService,
            BusinessService businessService,
            BookingTravellerTicketRepository bookingTravellerTicketRepository,
            BookingService bookingReadService,
            CreditLimitValidatorService creditLimitValidatorService,
            SupplierRepository supplierRepository,
            InvoiceLedgerRepository invoiceLedgerRepository,
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            AccountHeadRepository accountHeadRepository,
            SupplierTransactionHistoryRepository supplierTransactionHistoryRepository,
            BookingRepository bookingRepository,
            RoleAssignmentRepository roleAssignmentRepository,
            CoreBookingApiLogService coreBookingApiLogService,
            BookingCreationDuplicateGuardService bookingCreationDuplicateGuardService,
            PlatformProviderService platformProviderService,
            SupplierResolverService supplierResolverService,
            WebhookAlertDispatchService webhookAlertDispatchService,
            HoldToBookFailurePublisher holdToBookFailurePublisher,
            @Value("${core.booking.api.base-url}") String coreBookingBaseUrl,
            @Value("${core.booking.api.book-endpoint}") String coreBookingEndpoint,
            @Value("${core.booking.api.book-issue}") String coreIssueEndpoint,
            @Value("${core.booking.api.hold-to-book}") String coreHoldToBookEndpoint,
            @Value("${core.booking.api.repricing}") String coreRepricingEndpoint,
            @Value("${core.booking.api.cancel-booking}") String coreCancelBookingEndpoint,
            @Value("${flight_api_key}") String salt,
            @Value("${flight_api_url}") String flightApiUrl
    ) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.travellerService = travellerService;
        this.bookingPriceService = bookingPriceService;
        this.walletDepositRepository = walletDepositRepository;
        this.travelInformationRepository = travelInformationRepository;
        this.bookingSegmentRepository = bookingSegmentRepository;
        this.segmentAirportRepository = segmentAirportRepository;
        this.segmentAirlineRepository = segmentAirlineRepository;
        this.transactionRepository = transactionRepository;
        this.referenceGeneratorService = referenceGeneratorService;
        this.groupTicketService = groupTicketService;
        this.currencyService = currencyService;
        this.errorLogService = errorLogService;
        this.ticketingDeadlineService = ticketingDeadlineService;
        this.businessService = businessService;
        this.bookingTravellerTicketRepository = bookingTravellerTicketRepository;
        this.bookingReadService = bookingReadService;
        this.creditLimitValidatorService = creditLimitValidatorService;
        this.supplierRepository = supplierRepository;
        this.invoiceLedgerRepository = invoiceLedgerRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.accountHeadRepository = accountHeadRepository;
        this.supplierTransactionHistoryRepository = supplierTransactionHistoryRepository;
        this.bookingRepository = bookingRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.coreBookingApiLogService = coreBookingApiLogService;
        this.bookingCreationDuplicateGuardService = bookingCreationDuplicateGuardService;
        this.coreBookingBaseUrl = coreBookingBaseUrl;
        this.coreBookingEndpoint = coreBookingEndpoint;
        this.coreIssueEndpoint = coreIssueEndpoint;
        this.coreHoldToBookEndpoint = coreHoldToBookEndpoint;
        this.coreRepricingEndpoint = coreRepricingEndpoint;
        this.coreCancelBookingEndpoint = coreCancelBookingEndpoint;
        this.salt = salt;
        this.flightApiUrl = flightApiUrl;
        this.platformProviderService = platformProviderService;
        this.supplierResolverService = supplierResolverService;
        this.webhookAlertDispatchService = webhookAlertDispatchService;
        this.holdToBookFailurePublisher = holdToBookFailurePublisher;
    }


    @Transactional
    public BookingResponse create(BookingRequest req, Long userId, Long actingUserId, boolean bypassBilling) {
        String dedupKey = bookingCreationDuplicateGuardService.acquireOrThrow(req, userId);
        try {
            return createBooking(req, userId, actingUserId, bypassBilling);
        } catch (RuntimeException e) {
            bookingCreationDuplicateGuardService.release(dedupKey);
            throw e;
        }
    }

    private BookingResponse createBooking(BookingRequest req, Long userId, Long actingUserId, boolean bypassBilling) {
        User user = userService.getUser(userId);
        // For USBANGLAAPI with bundleCode, combine resultIndex with bundleCode for price lookup
        String priceKey = req.getResultIndex();
        if (
                (req.getProviderName() == Provider.USBANGLAAPI)
                        &&
                        req.getBundleCode() != null
                        &&
                        !req.getBundleCode().isBlank()
        ) {
            priceKey = req.getResultIndex() + ":" + req.getBundleCode();
            log.info("Using bundleCode-specific price key for USBANGLAAPI: " + priceKey);
        }


        // Get full price details including original price and markup
        BookingPriceService.BookingPriceDetails priceDetails = bookingPriceService.getBookingPriceDetails(
                priceKey, req.getProviderName().name().toLowerCase(),req.getChannel());

        Double bookingPrice = priceDetails.bookingPrice();
        Double originalPrice = priceDetails.originalPrice();
        Double markupAmount = priceDetails.markupAmount();
        Double buyPrice = priceDetails.buyPrice();

        if (req.getProviderName() == Provider.GROUP) {
            if (req.getGroupTicketType() == null || req.getGroupTicketType().isBlank()) {
                try {
                    GroupTicket groupTicket = groupTicketService.getGroupTicketEntity(req.getResultIndex());
                    if (groupTicket.getTicketType() != null && !groupTicket.getTicketType().isBlank()) {
                        req.setGroupTicketType(groupTicket.getTicketType());
                    }
                } catch (Exception e) {
                    log.warning("Could not resolve groupTicketType from GF code "
                            + req.getResultIndex() + ": " + e.getMessage());
                }
            }

            int adtCount = 0;
            int chdCount = 0;
            int infCount = 0;
            for (TravellerRequest traveller : req.getItineraries()) {
                if (traveller.calculatePassengerType().equalsIgnoreCase("ADT")) {
                    adtCount++;
                } else if (traveller.calculatePassengerType().equalsIgnoreCase("CHD")) {
                    chdCount++;
                } else if (traveller.calculatePassengerType().equalsIgnoreCase("INF")) {
                    infCount++;
                }
            }

            int totalPax = adtCount + chdCount + infCount;
            bookingPrice = groupTicketService.getPriceByGfCode(
                    req.getResultIndex(), adtCount, chdCount, infCount);

            Double costingPrice = groupTicketService.getCostingPriceByGfCode(req.getResultIndex(), totalPax);
            originalPrice = costingPrice != null ? costingPrice : bookingPrice;
            buyPrice = costingPrice != null ? costingPrice : originalPrice;
            markupAmount = bookingPrice - originalPrice;
        }


        if (bookingPrice == null || bookingPrice <= 0) {
            throw ServiceExceptions.bookingFailed("Failed to retrieve valid booking price for the selected flight");
        }

        log.info("💰 Retrieved booking price: " + bookingPrice + ", original: " + originalPrice
                + ", buy: " + buyPrice + ", markup: " + markupAmount);

        Double userBalance = userService.getUserBalance(userId);
        log.info("💰 User current balance userId " + userId + " : " + userBalance);
        double userExchangeRate = currencyService.getExchangeRateBasedOnUsd(
                user.getCurrency(), req.getProviderName().name(), req.getChannel());

        if (req.getBookType() == BookType.BOOK && !bypassBilling) {
            double convertedBookingPrice = bookingPrice * userExchangeRate;

            // Check balance including credit limit
            boolean hasEnoughBalance = creditLimitValidatorService.hasSufficientBalance(
                    userId, userBalance, convertedBookingPrice);

            if (!hasEnoughBalance) {
                double availableBalance = creditLimitValidatorService.getAvailableBalance(userId, userBalance);
                throw ServiceExceptions.insufficientBalance("Insufficient balance to create booking. Required: " +
                        Helper.formatMoney(convertedBookingPrice) + ", Available: " + Helper.formatMoney(availableBalance));
            }

        }

        log.info("✅ User has sufficient balance: " + userBalance);
        // now deduct the balance and create the booking


        List<Long> savedTravellerIds = Optional.ofNullable(req.getItineraries())
                .orElse(Collections.emptyList())
                .stream()
                .map(travellerRequest -> travellerService.createTraveller(travellerRequest, userId).getId())
                .toList();

        List<Long> allTravellerIds = new ArrayList<>();

        if (req.getTravellerIds() != null) {
            allTravellerIds.addAll(req.getTravellerIds());
        }

        allTravellerIds.addAll(savedTravellerIds);

        BookConformation bookConformation = new BookConformation();
        com.aerionsoft.application.dto.booking.TravelInformation travelInformation = new com.aerionsoft.application.dto.booking.TravelInformation();
        CoreResponse coreResponse = null;

        try {
            CoreBookingWrapper coreBookingWrapper = buildCoreBookingWrapper(req);
            log.info("📦 Built CoreBookingWrapper: " + coreBookingWrapper);
            log.info("Channel: " + req.getChannel() + ", Provider: " + req.getProviderName());
            coreResponse = callCoreApi(coreBookingWrapper, coreBookingEndpoint, CoreResponse.class);

            if (coreResponse == null || coreResponse.getPnr() == null) {
                throw ServiceExceptions.bookingFailed(coreResponse.getMessage() != null ? coreResponse.getMessage() : "Failed to create booking in core system");
            }

            if (coreResponse.getPnr().equalsIgnoreCase("UNKNOWN")) {
                throw ServiceExceptions.bookingFailed("Were unable to process your booking at the moment. Please try again later or contact support if the issue persists.");
            }


            bookConformation.setPnr(coreResponse.getPnr());
            bookConformation.setTicketNo(coreResponse.getTicketNo());
            bookConformation.setAirline(coreResponse.getAirline());
            bookConformation.setAirlinePnrs(coreResponse.getAirlinePnrs());
            bookConformation.setStatus(coreResponse.getStatus());
            bookConformation.setBookingDate(OffsetDateTime.now(ZoneOffset.UTC));
            bookConformation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            bookConformation.setFlightDetails(coreResponse.getFlightSSRList());
            bookConformation.setLastPaymentDate(coreResponse.getLastPaymentDate());
            bookConformation.setLastPaymentDateInSeconds(coreResponse.getSecondsUntilDeadline());
            bookConformation.setChannel(req.getChannel());
            bookConformation.setProviderBookingTime(coreResponse.getProviderBookingTime());
            bookConformation.setBookedTimeOffset(coreResponse.getBookedTimeOffset());
            bookConformation.setSourceType("ONLINE");
            log.info("✅ Booking created successfully in core system: " + bookConformation);

            // Extract travel information from core response if available
            if (coreResponse.getTravelInformation() != null) {
                // Use travel information directly from core response
                travelInformation = coreResponse.getTravelInformation();
            } else {
                // Populate basic information from the booking confirmation
                populateTravelInformationFromCoreResponse(travelInformation, coreResponse);

            }

        } catch (MicroserviceException e) {
            log.warning("❌ MicroserviceException during booking creation: " + e.getMessage());
            dispatchBookingCreateCoreFailureWebhook(req, user, coreResponse, e.getMessage());
            throw e;
        } catch (Exception e) {

            log.warning(e.getMessage());
            dispatchBookingCreateCoreFailureWebhook(req, user, coreResponse, e.getMessage());

            // Log the error to database
            errorLogService.logError(
                    "BOOKING_SERVICE",
                    "BOOKING_CREATION_FAILED",
                    e.getMessage(),
                    req,
                    null,
                    "500",
                    userId,
                    e
            );

            throw ServiceExceptions.bookingFailed( e.getMessage());
        }

        Transaction savedTxn = null;
        WalletDeposit savedDeposit = null;

        // Get user's preferred currency
        String userCurrency = user.getCurrency();
        if (userCurrency == null || userCurrency.isEmpty()) {
            userCurrency = "USD";
        }

        // Get the exchange rate from USD to user's currency
        String providerName = req.getProviderName() != null ? req.getProviderName().toString() : "DEFAULT";
        double exchangeRate;
        try {
            // Get exchange rate from USD to user's currency
            exchangeRate = currencyService.getExchangeRateBasedOnUsd(userCurrency, providerName, req.getChannel());
        } catch (Exception e) {
            // If getting exchange rate fails, log and use 1.00
            log.warning("Failed to get exchange rate: " + e.getMessage());
            exchangeRate = 1.0;
        }

        log.info("💱 Exchange rate from USD to " + userCurrency + " : " + exchangeRate);
        log.info(req.getBookType() + " | Bypass Billing: " + bypassBilling);
        if (req.getBookType() == BookType.BOOK && !bypassBilling) {

            // NEW: Deduct full amount from wallet - balance can go negative up to credit limit
            double convertedBookingPrice = bookingPrice * userExchangeRate;
            userService.deductUserBalance(userId, convertedBookingPrice, providerName, false,
                    "BookingCoordinatorService", null, "BOOKING", actingUserId);
            log.info("➖ Deducted " + convertedBookingPrice + " from wallet for booking (negative balance allowed with credit limit).");

            String depositReference = referenceGeneratorService.nextReference("FR");
            WalletDeposit deposit = WalletDeposit.builder()
                    .userId(userId)
                    .actingUserId(actingUserId)
                    .type(DepositType.PURCHASE)
                    .status(DepositStatus.APPROVED)
                    .amount(convertedBookingPrice)
                    .exchangeRate(exchangeRate)
                    .remarks(req.getProviderName().name().toLowerCase() + "booking_" + bookConformation.getPnr())
                    .reference(depositReference)
                    .transactionId(UUID.randomUUID().toString())
                    .createdAt(UserDateTimeUtil.now())
                    .currency(Currency.valueOf(userCurrency))
                    .exchangedAmount(convertedBookingPrice)
                    .build();

            savedDeposit = walletDepositRepository.save(deposit);

            Transaction tnx = Transaction.builder()
                    .type(DepositType.PURCHASE.name())
                    .amount(convertedBookingPrice)
                    .currency(userCurrency)
                    .exchangeRate(userExchangeRate)
                    .convertedAmount(String.valueOf(convertedBookingPrice))
                    .description("Deducted for " + req.getProviderName().name().toLowerCase() + " booking")
                    .userId(userId)
                    .createdBy("SYSTEM")
                    .createdAt(UserDateTimeUtil.now())
                    .sourceType(TransactionSourceType.BOOKING.name())
                    .reference(depositReference)
                    .active(true)
                    .build();

            savedTxn = transactionRepository.save(tnx);
        }


        // Create booking first without travel information
        BookingResponse bookingResponse = bookingService.create(
                req, userId, allTravellerIds, bookConformation,
                String.valueOf(bookingPrice), actingUserId,
                String.valueOf(originalPrice), String.valueOf(buyPrice), String.valueOf(markupAmount),
                user, exchangeRate, userCurrency);

        // Update transaction with booking source after booking is created
        if (savedTxn != null && bookingResponse.getId() != null) {
            savedTxn.linkSource(TransactionSourceType.BOOKING, bookingResponse.getId());
            savedTxn.setCurrency(bookingResponse.getExchangeCurrency());
            savedTxn.setExchangeRate(Double.valueOf(bookingResponse.getExchangeCurrencyRate()));
            savedTxn.setConvertedAmount(String.valueOf(bookingPrice * Double.parseDouble(bookingResponse.getExchangeCurrencyRate())));
            TransactionAuditHelper.touch(savedTxn, "SYSTEM");
            transactionRepository.save(savedTxn);
        }

        // Now create and save travel information with the booking ID
        TravelInformation savedTravelInfo = buildTraveller(travelInformation, bookingResponse.getId(), req.getSegments());

        // Add the travel information to the response
        if (savedTravelInfo != null) {
            com.aerionsoft.application.dto.booking.TravelInformation travelInfoDto = mapEntityToDto(savedTravelInfo);
            bookingResponse.setTravelInformation(travelInfoDto);
        }

        // Schedule ticketing deadline update for supported providers (Sabre, Verteil)
        if ((req.getProviderName() == Provider.SABRE || req.getProviderName() == Provider.VERTEIL) &&
                bookingResponse.getId() != null &&
                bookConformation.getPnr() != null && bookConformation.getLastPaymentDate() == null ||
                bookConformation.getLastPaymentDateInSeconds() == null
                        &&
                        bookConformation.getChannel() != null) {

            log.info("Scheduling ticketing deadline update for booking ID: " + bookingResponse.getId() +
                    ", Provider: " + req.getProviderName() +
                    ", PNR: " + bookConformation.getPnr() +
                    ", Channel: " + bookConformation.getChannel());

            ticketingDeadlineService.scheduleTicketingDeadlineUpdate(
                    bookingResponse.getId(),
                    req.getProviderName(),
                    bookConformation.getPnr(),
                    bookConformation.getChannel()
            );
        }

        // Enrich travellers with individual ticket numbers from the core response
        enrichTravellersWithTicketNumbers(bookingResponse, coreResponse.getTickets());

        // Auto-create supplier invoice when booking is immediately CONFIRMED/TICKETED
        if (bookingResponse.getId() != null
                && bookConformation.getStatus() != null
                && isInvoiceableBookingStatus(bookConformation.getStatus())) {
            Booking savedBooking = bookingService.getBookingById(bookingResponse.getId());
            BookingInvoiceContext invoiceContext = buildInvoiceContextFromOnline(
                    savedBooking, req, bookConformation.getTicketNo(), originalPrice, buyPrice);
            tryCreateSupplierInvoice(savedBooking, invoiceContext, bookConformation.getTicketNo());
        }

        return bookingResponse;
    }

    /**
     * Assigns each ticket number to the corresponding traveller by passenger order.
     * Persists the ticket number in booking_traveller_tickets and updates the response travellers list.
     */
    private void enrichTravellersWithTicketNumbers(BookingResponse bookingResponse,
                                                   List<PassengerTicketDTO> tickets) {
        if (tickets == null || tickets.isEmpty() || bookingResponse.getId() == null) {
            return;
        }

        List<Long> travellerIds = bookingResponse.getTravellerIds();
        if (travellerIds == null || travellerIds.isEmpty()) {
            return;
        }

        List<TravellerResponse> travellers = resolveOrderedTravellers(travellerIds);
        assignTravellerTicketNumbersByOrder(bookingResponse.getId(), travellers, tickets);
        bookingResponse.setTravellers(travellers);
    }


    private TravelInformation buildTraveller(com.aerionsoft.application.dto.booking.TravelInformation travelInformation, Long bookingId, List<SegmentRequest> segments) {
        if (travelInformation == null) {
            return null;
        }

        TravelInformation entity = TravelInformation.builder()
                .bookingId(bookingId)
                .airlineName(travelInformation.getAirlineName())
                .flightNumber(travelInformation.getFlightNumber())
                .origin(travelInformation.getOrigin())
                .destination(travelInformation.getDestination())
                .departureAirport(travelInformation.getDepartureAirport())
                .arrivalAirport(travelInformation.getArrivalAirport())
                .departureDate(travelInformation.getDepartureDate())
                .departureTime(travelInformation.getDepartureTime())
                .arrivalDate(travelInformation.getArrivalDate())
                .arrivalTime(travelInformation.getArrivalTime())
                .fareBasis(travelInformation.getFareBasis())
                .quantity(travelInformation.getQuantity())
                .currency(travelInformation.getCurrency())
                .baseFare(travelInformation.getBaseFare())
                .equivalentBaseFare(travelInformation.getEquivalentBaseFare())
                .baggageKg(travelInformation.getBaggageKg())
                .tax(travelInformation.getTax())
                .duration(travelInformation.getDuration())
                .ticketNumber(travelInformation.getTicketNumber())
                .instructions(travelInformation.getInstructions())
                .flightType(travelInformation.getFlightType())
                .airlineCode(travelInformation.getAirlineCode())
                .onewaySegmentStopCount(travelInformation.getOnewaySegmentStopCount())
                .returnSegmentStopCount(travelInformation.getReturnSegmentStopCount())
                .build();

        TravelInformation savedEntity = travelInformationRepository.save(entity);

        // Save segments if provided
        if (segments != null && !segments.isEmpty()) {
            saveSegments(segments, savedEntity.getId());
        }

        return savedEntity;
    }

    /**
     * Saves flight segments associated with travel information
     * Each segment saves: 1 segment record, 2 airport records (origin/destination), 1 airline record
     */
    private void saveSegments(List<SegmentRequest> segments, Long travelInformationId) {
        int order = 0;
        for (SegmentRequest segmentReq : segments) {
            // 1. Save the main segment record
            BookingSegment segment = BookingSegment.builder()
                    .travelInformationId(travelInformationId)
                    .baggagePieceCount(segmentReq.getBaggagePieceCount())
                    .duration(segmentReq.getDuration())
                    .cabinClass(segmentReq.getCabinClass())
                    .noOfSeatAvailable(segmentReq.getNoOfSeatAvailable())
                    .cabinBaggage(segmentReq.getCabinBaggage())
                    .baggage(segmentReq.getBaggage())
                    .bookingCode(segmentReq.getBookingCode())
                    .segmentOrder(segmentReq.getSegmentOrder() != null ? segmentReq.getSegmentOrder() : order++)
                    .segmentType(segmentReq.getSegmentType())
                    .build();

            BookingSegment savedSegment = bookingSegmentRepository.save(segment);
            Long segmentId = savedSegment.getId();

            segmentAirportRepository.deleteBySegmentId(segmentId);
            segmentAirlineRepository.deleteBySegmentId(segmentId);

            // 2. Save Origin Airport
            if (segmentReq.getOrigin() != null) {
                SegmentAirport originAirport = SegmentAirport.builder()
                        .segmentId(segmentId)
                        .airportType("ORIGIN")
                        .time(segmentReq.getOrigin().getDepTime())
                        .build();

                if (segmentReq.getOrigin().getAirport() != null) {
                    SegmentRequest.AirportInfo airportInfo = segmentReq.getOrigin().getAirport();
                    originAirport.setAirportCode(airportInfo.getAirportCode());
                    originAirport.setAirportName(airportInfo.getAirportName());
                    originAirport.setTerminal(airportInfo.getTerminal());
                    originAirport.setCityCode(airportInfo.getCityCode());
                    originAirport.setCityName(airportInfo.getCityName());
                    originAirport.setCountryCode(airportInfo.getCountryCode());
                    originAirport.setCountryName(airportInfo.getCountryName());
                }

                segmentAirportRepository.save(originAirport);
            }

            // 3. Save Destination Airport
            if (segmentReq.getDestination() != null) {
                SegmentAirport destAirport = SegmentAirport.builder()
                        .segmentId(segmentId)
                        .airportType("DESTINATION")
                        .time(segmentReq.getDestination().getArrTime())
                        .build();

                if (segmentReq.getDestination().getAirport() != null) {
                    SegmentRequest.AirportInfo airportInfo = segmentReq.getDestination().getAirport();
                    destAirport.setAirportCode(airportInfo.getAirportCode());
                    destAirport.setAirportName(airportInfo.getAirportName());
                    destAirport.setTerminal(airportInfo.getTerminal());
                    destAirport.setCityCode(airportInfo.getCityCode());
                    destAirport.setCityName(airportInfo.getCityName());
                    destAirport.setCountryCode(airportInfo.getCountryCode());
                    destAirport.setCountryName(airportInfo.getCountryName());
                }

                segmentAirportRepository.save(destAirport);
            }

            // 4. Save Airline
            if (segmentReq.getAirline() != null) {
                SegmentRequest.AirlineInfo airlineInfo = segmentReq.getAirline();
                SegmentAirline airline = SegmentAirline.builder()
                        .segmentId(segmentId)
                        .airlineCode(airlineInfo.getAirlineCode())
                        .airlineName(airlineInfo.getAirlineName())
                        .flightNumber(airlineInfo.getFlightNumber())
                        .fareClass(airlineInfo.getFareClass())
                        .operatingCarrier(airlineInfo.getOperatingCarrier())
                        .build();

                segmentAirlineRepository.save(airline);
            }
        }
    }

    public <Req, Res> Res callCoreApi(Req request, String endpoint, Class<Res> responseType) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-API-KEY", salt);

        HttpEntity<Req> requestEntity = new HttpEntity<>(request, headers);
        String sessionId = UUID.randomUUID().toString();

        String url = coreBookingBaseUrl + endpoint + "?sessionId=" + sessionId;

        Res responseBody = null;
        String httpStatus = null;
        String logStatus = "SUCCESS";
        String errorMessage = null;

        try {
            ResponseEntity<Res> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    responseType
            );

            httpStatus = String.valueOf(response.getStatusCode().value());

            if (response.getStatusCode().is2xxSuccessful()) {
                responseBody = response.getBody();
                if (responseBody != null) {
                    log.info("✅ API Call Successful: " + responseBody);
                    return responseBody;
                }
                logStatus = "FAILED";
                errorMessage = "API returned empty response";
                throw new MicroserviceException(MicroserviceType.CORE_BOOKING,
                        "EMPTY_RESPONSE", errorMessage);
            }

            logStatus = "FAILED";
            errorMessage = "API failed with status: " + response.getStatusCode();
            throw new MicroserviceException(MicroserviceType.CORE_BOOKING,
                    "HTTP_ERROR", errorMessage);

        } catch (MicroserviceException ex) {
            if ("SUCCESS".equals(logStatus)) {
                logStatus = "FAILED";
                errorMessage = ex.getMessage();
            }
            throw ex;
        } catch (Exception ex) {
            log.warning("❌ API Call Error: " + ex.getMessage());
            logStatus = "ERROR";
            errorMessage = ex.getMessage();
            throw new MicroserviceException(MicroserviceType.CORE_BOOKING,
                    "API_CALL_ERROR", "Error calling API: " + ex.getMessage(), ex);
        } finally {
            coreBookingApiLogService.log(
                    endpoint,
                    sessionId,
                    request,
                    responseBody,
                    httpStatus,
                    logStatus,
                    errorMessage);
        }
    }

    /**
     * Retrieves a reservation from the flight API.
     */
    public BaseResponse<GetReservationResponse> getReservation(String sessionId, GetReservationRequest request) {
        String channel = platformProviderService.getChannelName(request.getProvider().getValue());
        if (channel == null || channel.isBlank()) {
            return BaseResponse.error("No API channel mapped for provider: " + request.getProvider());
        }
        request.setChannel(channel);

        User user = userService.getUser(request.getMotherUserId());
        String userCurrency = user.getCurrency();
        double userExchangeRate = currencyService.getExchangeRateBasedOnUsd(
                userCurrency, request.getProvider().name(), channel);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("x-api-key", salt);

        HttpEntity<GetReservationRequest> requestEntity = new HttpEntity<>(request, headers);
        String url = flightApiUrl + "/api/flights/get-reservation?sessionId=" + sessionId;

        ResponseEntity<BaseResponse<java.util.Map<String, Object>>> rawResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<BaseResponse<java.util.Map<String, Object>>>() {
                }
        );

        BaseResponse<java.util.Map<String, Object>> body = rawResponse.getBody();
        if (body == null || body.getData() == null) {
            return BaseResponse.error("No reservation data returned");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        GetReservationResponse dto = objectMapper.convertValue(body.getData(), GetReservationResponse.class);

        // Apply exchange rate to price fields and update currency
        if (dto.getOriginalPrice() != null) {
            dto.setOriginalPrice(dto.getOriginalPrice() * userExchangeRate);
        }
        if (dto.getMarkupAmount() != null) {
            dto.setMarkupAmount(dto.getMarkupAmount() * userExchangeRate);
        }
        if (dto.getTaxAmount() != null) {
            dto.setTaxAmount(dto.getTaxAmount() * userExchangeRate);
        }
        dto.setCurrency(userCurrency);
        dto.setProviderName(request.getProvider().name());

        return BaseResponse.ok(body.getMessage(), dto);
    }

    /**
     * Loads booking / issuance list from the flight API (channel + date).
     * Channel is resolved from the cached platform provider map.
     */
    public BaseResponse<Map<String, Object>> loadBooking(String sessionId, LoadBookingRequest request) {
        String channel = platformProviderService.getChannelName(request.getProvider().getValue());
        if (channel == null || channel.isBlank()) {
            return BaseResponse.error("No API channel mapped for provider: " + request.getProvider());
        }

        Map<String, String> upstreamBody = new HashMap<>();
        upstreamBody.put("channel", channel);
        upstreamBody.put("date", request.getDate());

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("x-api-key", salt);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(upstreamBody, headers);
        String url = flightApiUrl + "/api/flights/load-booking?sessionId=" + sessionId;

        ResponseEntity<BaseResponse<Map<String, Object>>> rawResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<BaseResponse<Map<String, Object>>>() {
                }
        );

        BaseResponse<Map<String, Object>> body = rawResponse.getBody();
        if (body == null) {
            return BaseResponse.error("Load booking upstream returned empty response");
        }
        if (body.getData() != null) {
            filterLoadBookingIssuanceAlreadyInSystem(body.getData());
        }
        return body;
    }

    /**
     * Drops issuance rows whose PNR already exists on a booking in CONFIRMED or TICKETED status.
     */
    @SuppressWarnings("unchecked")
    private void filterLoadBookingIssuanceAlreadyInSystem(Map<String, Object> data) {
        Object raw = data.get("issuanceData");
        if (!(raw instanceof List<?> issuanceList)) {
            return;
        }
        if (issuanceList.isEmpty()) {
            data.put("excludedPnrCount", 0);
            data.put("excludedPnrs", List.of());
            return;
        }

        Set<String> pnrsToCheck = new HashSet<>();
        for (Object item : issuanceList) {
            if (item instanceof Map<?, ?> map) {
                Object pnrObj = map.get("pnr");
                if (pnrObj instanceof String s && !s.isBlank()) {
                    pnrsToCheck.add(s.trim());
                }
            }
        }
        if (pnrsToCheck.isEmpty()) {
            data.put("excludedPnrCount", 0);
            data.put("excludedPnrs", List.of());
            return;
        }

        List<BookingStatus> terminalStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.TICKETED);
        List<Booking> existing = bookingRepository.findByPnrIgnoreCaseInAndStatusIn(pnrsToCheck, terminalStatuses);
        Set<String> excludeNormalized = new HashSet<>();
        for (Booking b : existing) {
            if (b.getPnr() != null && !b.getPnr().isBlank()) {
                excludeNormalized.add(b.getPnr().trim().toUpperCase(Locale.ROOT));
            }
        }
        if (excludeNormalized.isEmpty()) {
            data.put("excludedPnrCount", 0);
            data.put("excludedPnrs", List.of());
            return;
        }

        LinkedHashSet<String> excludedPnrsOrdered = new LinkedHashSet<>();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Object item : issuanceList) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object pnrObj = map.get("pnr");
            String key = (pnrObj instanceof String s && !s.isBlank()) ? s.trim().toUpperCase(Locale.ROOT) : "";
            if (excludeNormalized.contains(key)) {
                if (pnrObj instanceof String s && !s.isBlank()) {
                    excludedPnrsOrdered.add(s.trim());
                }
            } else {
                filtered.add((Map<String, Object>) map);
            }
        }
        data.put("issuanceData", filtered);
        data.put("excludedPnrCount", excludedPnrsOrdered.size());
        data.put("excludedPnrs", new ArrayList<>(excludedPnrsOrdered));
    }

    private CoreBookingWrapper buildCoreBookingWrapper(BookingRequest booking) {
        List<TravellerRequest> itineraries = booking.getItineraries();

        CoreBookingWrapper wrapper = new CoreBookingWrapper();
        wrapper.setType(booking.getBookType());
        wrapper.setResultIndex(booking.getResultIndex());
        wrapper.setBundleCode(booking.getBundleCode()); // For USBANGLAAPI
        wrapper.setChannel(booking.getChannel());
        wrapper.setProviderName(String.valueOf(booking.getProviderName()));

        List<CoreBookingRequest> coreBookingRequest = new ArrayList<>();
        for (TravellerRequest itinerary : itineraries) {
            CoreBookingRequest coreBooking = new CoreBookingRequest();

            coreBooking.setDob(itinerary.getDob());
            coreBooking.setEmail(itinerary.getEmail());
            coreBooking.setFirstName(itinerary.getFirstName());
            coreBooking.setLastName(itinerary.getLastName());
            coreBooking.setGender(itinerary.getGender());
            coreBooking.setAddressLine1(itinerary.getAddressLine1());
            coreBooking.setAddressLine2(itinerary.getAddressLine2());
            coreBooking.setMobile(itinerary.getMobile());
            coreBooking.setMobileCountryCode(itinerary.getMobileCountryCode());
            coreBooking.setNationality(itinerary.getCountryName());
            coreBooking.setPassportNo(itinerary.getPassportNo());
            coreBooking.setPassportIssueDate(itinerary.getPassportIssueDate());
            coreBooking.setPassportExpiryDate(itinerary.getPassportExpiryDate());
            coreBooking.setTitle(itinerary.getTitle());
            coreBooking.setCountryName(itinerary.getCountryName());
            coreBooking.setCountryCode(itinerary.getCountryCode());
            coreBooking.setCityName(itinerary.getCityName());
            coreBooking.setCityCode(itinerary.getCityCode());
            coreBooking.setMealCode(itinerary.getMealCode());
            coreBooking.setBookingClass(String.valueOf(booking.getBookingClass().getValue()));

            coreBookingRequest.add(coreBooking);
        }

        wrapper.setBookInfoList(coreBookingRequest);
        return wrapper;
    }

    /**
     * Populates travel information from core response
     */
    private void populateTravelInformationFromCoreResponse(com.aerionsoft.application.dto.booking.TravelInformation travelInformation, CoreResponse coreResponse) {
        // This method can be enhanced based on what data is available in CoreResponse
        // For now, we'll populate basic information from the booking confirmation
        if (coreResponse.getTicketNo() != null) {
            travelInformation.setTicketNumber(coreResponse.getTicketNo());
        }
        if (coreResponse.getAirline() != null) {
            travelInformation.setAirlineName(coreResponse.getAirline());
        }
        // Add more mappings based on available fields in CoreResponse
    }

    /**
     * Maps TravelInformation entity to DTO
     */
    private com.aerionsoft.application.dto.booking.TravelInformation mapEntityToDto(TravelInformation entity) {
        if (entity == null) {
            return null;
        }

        com.aerionsoft.application.dto.booking.TravelInformation dto = new com.aerionsoft.application.dto.booking.TravelInformation();
        dto.setId(entity.getId());
        dto.setAirlineName(entity.getAirlineName());
        dto.setFlightNumber(entity.getFlightNumber());
        dto.setOrigin(entity.getOrigin());
        dto.setDestination(entity.getDestination());
        dto.setDepartureAirport(entity.getDepartureAirport());
        dto.setArrivalAirport(entity.getArrivalAirport());
        dto.setDepartureDate(entity.getDepartureDate());
        dto.setDepartureTime(entity.getDepartureTime());
        dto.setArrivalDate(entity.getArrivalDate());
        dto.setArrivalTime(entity.getArrivalTime());
        dto.setFareBasis(entity.getFareBasis());
        dto.setQuantity(entity.getQuantity());
        dto.setCurrency(entity.getCurrency());
        dto.setBaseFare(entity.getBaseFare());
        dto.setEquivalentBaseFare(entity.getEquivalentBaseFare());
        dto.setBaggageKg(entity.getBaggageKg());
        dto.setTax(entity.getTax());
        dto.setDuration(entity.getDuration());
        dto.setTicketNumber(entity.getTicketNumber());
        dto.setInstructions(entity.getInstructions());
        dto.setFlightType(entity.getFlightType());
        dto.setAirlineCode(entity.getAirlineCode());
        dto.setOnewaySegmentStopCount(entity.getOnewaySegmentStopCount());
        dto.setReturnSegmentStopCount(entity.getReturnSegmentStopCount());

        // Load segments with nested objects
        List<BookingSegment> segments = bookingSegmentRepository.findByTravelInformationIdOrderBySegmentOrderAsc(entity.getId());
        if (segments != null && !segments.isEmpty()) {
            List<Long> segmentIds = segments.stream().map(BookingSegment::getId).toList();
            List<SegmentAirport> allAirports = segmentAirportRepository.findBySegmentIdIn(segmentIds);
            Map<Long, SegmentAirport> originsBySegmentId = indexAirportsBySegment(allAirports, "ORIGIN");
            Map<Long, SegmentAirport> destinationsBySegmentId = indexAirportsBySegment(allAirports, "DESTINATION");
            Map<Long, SegmentAirline> airlinesBySegmentId = indexAirlinesBySegment(
                    segmentAirlineRepository.findBySegmentIdIn(segmentIds));

            List<SegmentDTO> segmentDTOs = segments.stream()
                    .map(segment -> mapSegmentToDto(
                            segment,
                            originsBySegmentId.get(segment.getId()),
                            destinationsBySegmentId.get(segment.getId()),
                            airlinesBySegmentId.get(segment.getId())))
                    .toList();
            dto.setSegments(segmentDTOs);
        }

        return dto;
    }

    private Map<Long, SegmentAirport> indexAirportsBySegment(List<SegmentAirport> airports, String airportType) {
        Map<Long, SegmentAirport> indexed = new HashMap<>();
        for (SegmentAirport airport : airports) {
            if (!airportType.equals(airport.getAirportType())) {
                continue;
            }
            indexed.merge(airport.getSegmentId(), airport,
                    (existing, candidate) -> existing.getId() <= candidate.getId() ? existing : candidate);
        }
        return indexed;
    }

    private Map<Long, SegmentAirline> indexAirlinesBySegment(List<SegmentAirline> airlines) {
        Map<Long, SegmentAirline> indexed = new HashMap<>();
        for (SegmentAirline airline : airlines) {
            indexed.merge(airline.getSegmentId(), airline,
                    (existing, candidate) -> existing.getId() <= candidate.getId() ? existing : candidate);
        }
        return indexed;
    }

    /**
     * Maps BookingSegment entity to SegmentDTO with nested origin, destination, and airline
     */
    private SegmentDTO mapSegmentToDto(BookingSegment segment,
                                       SegmentAirport originAirport,
                                       SegmentAirport destAirport,
                                       SegmentAirline airline) {
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

        if (originAirport != null) {
            dto.setOrigin(mapAirportToDto(originAirport));
        }

        if (destAirport != null) {
            dto.setDestination(mapAirportToDto(destAirport));
        }

        if (airline != null) {
            dto.setAirline(mapAirlineToDto(airline));
        }

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


    public String issue(Long bookingId, Long userId, String correctedSalt) {
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        if (booking.getStatus() != BookingStatus.PNR) {
            throw ServiceExceptions.invalidState("Only pending bookings can be issued. Current status: " + booking.getStatus());
        }

        IssueTicket issueTicket = BuildCoreIssueWrapper(booking);
        // call core issue api
        CoreResponse coreResponse = callCoreApi(issueTicket, coreIssueEndpoint, CoreResponse.class);
        if (coreResponse.getStatus() == BookingStatus.TICKET_ISSUED || coreResponse.getStatus() == BookingStatus.CONFIRMED) {
            bookingService.updatePnrAndTicketNo(bookingId, coreResponse.getPnr(), coreResponse.getTicketNo(), BookingStatus.TICKET_ISSUED, "Ticket issued successfully", coreResponse.getAirlinePnrs());
            log.info("✅ Ticket issued successfully for booking ID " + bookingId + ": " + coreResponse);
            // Persist per-traveller ticket numbers so fetch booking API returns them
            if (coreResponse.getTickets() != null && !coreResponse.getTickets().isEmpty()) {
                persistTravellerTicketNumbers(booking, coreResponse.getTickets());
            }
            return "Ticket issued successfully. Ticket No: " + coreResponse.getTicketNo();
        } else {
            bookingService.updatePnrAndTicketNo(bookingId, booking.getPnr(), booking.getTicketNo(), BookingStatus.FAILED, coreResponse.getReason(), null);
            throw ServiceExceptions.ticketIssueFailed("Ticket issue failed: " + coreResponse.getReason());
        }

    }

    private IssueTicket BuildCoreIssueWrapper(Booking booking) {
        IssueTicket issueTicket = new IssueTicket();
        issueTicket.setPnr(booking.getPnr());
        issueTicket.setProviderName(booking.getProviderName().name().toLowerCase());
        issueTicket.setPrice_change_accepted("yes");
        issueTicket.setRemarks("Not Required");
        return issueTicket;
    }

    /**
     * Loads travellers from the booking entity and assigns ticket numbers by passenger order.
     * Called after a successful issue() so fetch calls immediately return ticket numbers.
     */
    private void persistTravellerTicketNumbers(Booking booking, List<PassengerTicketDTO> tickets) {
        if (booking.getTravellerIds() == null || booking.getTravellerIds().isBlank()) {
            return;
        }

        List<Long> ids = Helper.parseIds(booking.getTravellerIds());
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<TravellerResponse> travellers = resolveOrderedTravellers(ids);
        assignTravellerTicketNumbersByOrder(booking.getId(), travellers, tickets);
    }

    private List<TravellerResponse> resolveOrderedTravellers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Map<Long, TravellerResponse> travellersById = travellerService.getTravellersByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(TravellerResponse::getId, t -> t, (a, b) -> a));
        return ids.stream()
                .map(travellersById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private void assignTravellerTicketNumbersByOrder(Long bookingId,
                                                     List<TravellerResponse> travellers,
                                                     List<PassengerTicketDTO> tickets) {
        if (tickets == null || tickets.isEmpty() || travellers == null || travellers.isEmpty()) {
            return;
        }

        int limit = Math.min(travellers.size(), tickets.size());
        for (int i = 0; i < limit; i++) {
            TravellerResponse traveller = travellers.get(i);
            PassengerTicketDTO ticket = tickets.get(i);
            if (ticket.getTicketNumber() == null || ticket.getTicketNumber().isBlank()) {
                continue;
            }

            traveller.setTicketNumber(ticket.getTicketNumber());
            log.info("🎫 Assigned ticket " + ticket.getTicketNumber() +
                    " to traveller index " + i + " (id=" + traveller.getId() + ")");

            if (bookingId != null && traveller.getId() != null) {
                try {
                    upsertTravellerTicket(bookingId, traveller.getId(), ticket.getTicketNumber());
                } catch (Exception e) {
                    log.warning("⚠️ Could not persist ticket for traveller " +
                            traveller.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Upserts a ticket number into booking_traveller_tickets for the given booking + traveller pair.
     */
    private void upsertTravellerTicket(Long bookingId, Long travellerId, String ticketNumber) {
        BookingTravellerTicket record = bookingTravellerTicketRepository
                .findByBookingIdAndTravellerId(bookingId, travellerId)
                .orElseGet(() -> BookingTravellerTicket.builder()
                        .bookingId(bookingId)
                        .travellerId(travellerId)
                        .build());
        record.setTicketNumber(ticketNumber);
        bookingTravellerTicketRepository.save(record);
    }


    private CoreHoldToBooKRequest buildCoreRequestForHoldToBook(Booking booking) {

        List<Long> travellerIds = new ArrayList<>();
        CoreHoldToBooKRequest coreHoldToBooKRequest = new CoreHoldToBooKRequest();
        if (!booking.getTravellerIds().isEmpty() && booking.getProviderName().name().equalsIgnoreCase("sabre")) {
            travellerIds = Arrays.stream(booking.getTravellerIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try {
                            return Long.valueOf(s);
                        } catch (NumberFormatException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            List<TravellerResponse> travellers = travellerService.getTravellersByIds(travellerIds);

            int adultCount = 0;
            int childc03Count = 0;
            int childc07Count = 0;
            int infantCount = 0;

            for (TravellerResponse traveller : travellers) {
                String category = calculatePassengerCategory(traveller.getDob());

                if (category.startsWith("INF")) {
                    infantCount++;
                    continue;
                }

                switch (category) {
                    case "ADT" -> adultCount++;
                    case "C03" -> childc03Count++;
                    case "C07" -> childc07Count++;
                    // other categories fall through with no action
                }
            }
            log.info("👨‍👩‍👧‍👦 Passenger counts - Adults: " + adultCount + ", C03: " + childc03Count + ", C07: " + childc07Count + ", Infants: " + infantCount);
            List<Record> recordList = new ArrayList<>();
            if (adultCount > 0) {
                Record adultRecord = new Record();
                adultRecord.setCount(adultCount);
                adultRecord.setType(PassenserEnum.ADULT);
                recordList.add(adultRecord);
            }
            if (childc03Count > 0) {
                Record c03Record = new Record();
                c03Record.setCount(childc03Count);
                c03Record.setType(PassenserEnum.CHILD03);
                recordList.add(c03Record);
            }
            if (childc07Count > 0) {
                Record c07Record = new Record();
                c07Record.setCount(childc07Count);
                c07Record.setType(PassenserEnum.CHILD07);
                recordList.add(c07Record);
            }
            if (infantCount > 0) {
                Record infantRecord = new Record();
                infantRecord.setCount(infantCount);
                infantRecord.setType(PassenserEnum.INFANT);
                recordList.add(infantRecord);
            }
            coreHoldToBooKRequest.setRecords(recordList);

        }
        coreHoldToBooKRequest.setOriginalPrice(Double.valueOf(booking.getOriginalPrice()));
        coreHoldToBooKRequest.setChannel(booking.getChannel());
        coreHoldToBooKRequest.setPnr(booking.getPnr());
        coreHoldToBooKRequest.setProviderName(booking.getProviderName().name());
        return coreHoldToBooKRequest;

    }


    public String calculatePassengerCategory(LocalDate dob) {
        LocalDate now = LocalDate.now();

        Period period = Period.between(dob, now);
        int years = period.getYears();
        int months = period.getYears() * 12 + period.getMonths();

        if (years < 2) {
            return "INF" + months;  // Example: INF3, INF15, INF23
        }

        if (years >= 2 && years <= 4) {
            return "C03";
        }

        if (years >= 5 && years <= 11) {
            return "C07";
        }

        return "ADT";
    }

    /**
     * Creates a manual booking without calling external GDS/provider APIs
     * Used for offline/manual ticket bookings where PNR/ticket details are entered directly
     */
    @Transactional
    public BookingResponse createManualBooking(ManualBookingRequest req, Long adminUserId) {


        Supplier supplier =
                supplierResolverService.resolveForManualBooking(req.getSupplierId());

        String supplierCurrency =  supplier.getBranch().getCurrency();

        if(supplierCurrency.isEmpty()){
            throw new ServiceException("Supplier currency is empty.Please set the currency for supplier branch.");
        }


        // Validate required fields for manual booking
        validateManualBookingRequest(req);
        BusinessDto businessDto = businessService.getBusinessByUserId(req.getAgencyId());

        // Get user for balance check and booking creation
        User user = userService.getUser(businessDto.getMotherUserId());
        Long userId = user.getId();
        log.info("📝 Creating manual booking for user: " + user.getId());

        Double bookingPrice = req.getBookingPrice();
        Double originalPrice = req.getOriginalPrice() != null ? req.getOriginalPrice() : bookingPrice;
        Double buyPrice = req.getBuyPrice() != null ? req.getBuyPrice() : originalPrice;

        double markupAmount = bookingPrice - originalPrice;

        // Check user balance if deduction is enabled
        if (req.isDeductFromWallet()) {
            boolean canOverrideBalance = hasAdminOverrideBalancePermission(adminUserId);
            Double userBalance = userService.getUserBalance(userId);
            log.info("💰 User current balance userId " + userId + " : " + userBalance);
            if (!canOverrideBalance
                    && !creditLimitValidatorService.hasSufficientBalance(userId, userBalance, bookingPrice)) {
                double availableBalance = creditLimitValidatorService.getAvailableBalance(userId, userBalance);
                throw ServiceExceptions.business("Insufficient wallet balance. Required: " + Helper.formatMoney(bookingPrice) + ", Available: " +
                                Helper.formatMoney(availableBalance));
            }
        }

        // Create or reuse travellers from itineraries
        List<Long> savedTravellerIds = Optional.ofNullable(req.getItineraries())
                .orElse(Collections.emptyList())
                .stream()
                .map(travellerRequest -> travellerService.createOrUpdateTraveller(travellerRequest, userId).getId())
                .toList();

        List<Long> allTravellerIds = new ArrayList<>();

        if (req.getTravellerIds() != null) {
            allTravellerIds.addAll(req.getTravellerIds());
        }
        allTravellerIds.addAll(savedTravellerIds);

        if (allTravellerIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "At least one traveller is required for booking");
        }

        // Get user's preferred currency
        String userCurrency = user.getCurrency();
        if (userCurrency == null || userCurrency.isEmpty()) {
            userCurrency = req.getCurrency() != null ? req.getCurrency() : "USD";
        }
        FinalFare fare = new FinalFare();
        fare.setBaseFareCurrency(supplierCurrency);

        String providerName = req.getProviderName() != null ? req.getProviderName().name() : Provider.OTHERS.name();
        // Get exchange rate
        double exchangeRate = 1.00;
        try {
            exchangeRate = currencyService.getExchangeRateBasedOnUsd(userCurrency, providerName, req.getChannel());
        } catch (Exception e) {
            log.warning("Failed to get exchange rate: " + e.getMessage());
        }

        Double supplierCurrencyRate = currencyService.getExchangeRateBasedOnUsd(supplierCurrency, providerName, req.getChannel());

        fare.setFareExchangeRate(supplierCurrencyRate);

        // Process wallet deduction if enabled
        Transaction savedTxn = null;
        Double originalPriceTOUsd = originalPrice / exchangeRate;
        Double buyPriceInUsd = buyPrice / exchangeRate;
        Double bookingPriceInUsd = bookingPrice / exchangeRate;
        Double markupPriceInUsd = markupAmount / exchangeRate;

        if (req.isDeductFromWallet()) {
            boolean canOverrideBalance = hasAdminOverrideBalancePermission(adminUserId);
            userService.deductUserBalance(userId, bookingPrice, providerName, canOverrideBalance);
            log.info("➖ Deducted " + bookingPrice + " from wallet for manual booking. Override used: " + canOverrideBalance);


            String depositReference = referenceGeneratorService.nextReference("FR");

            WalletDeposit deposit = WalletDeposit.builder()
                    .userId(userId)
                    .actingUserId(adminUserId)
                    .type(DepositType.PURCHASE)
                    .status(DepositStatus.APPROVED)
                    .amount(bookingPriceInUsd)
                    .exchangeRate(1.0)
                    .remarks("manual_booking_" + (req.getPnr() != null ? req.getPnr() : "no_pnr"))
                    .reference(depositReference)
                    .transactionId(UUID.randomUUID().toString())
                    .createdAt(UserDateTimeUtil.now())
                    .exchangedAmount(1.0)
                    .build();

            walletDepositRepository.save(deposit);

            Transaction tnx = Transaction.builder()
                    .type(DepositType.PURCHASE.name())
                    .amount(bookingPrice)
                    .currency(userCurrency)
                    .exchangeRate(exchangeRate)
                    .convertedAmount(String.valueOf(bookingPriceInUsd))
                    .description("Deducted for manual booking")
                    .userId(userId)
                    .createdBy("SYSTEM")
                    .createdAt(UserDateTimeUtil.now())
                    .sourceType(TransactionSourceType.BOOKING.name())
                    .reference(depositReference)
                    .build();

            savedTxn = transactionRepository.save(tnx);
        }

        // Build booking conformation object for manual booking
        BookConformation bookConformation = new BookConformation();
        bookConformation.setPnr(req.getPnr());
        bookConformation.setTicketNo(req.getTicketNo());
        bookConformation.setAirline(req.getAirline());
        bookConformation.setStatus(req.getStatus());
        bookConformation.setBookingDate(req.getBookingDate() != null ? req.getBookingDate() : OffsetDateTime.now(ZoneOffset.UTC));
        bookConformation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        bookConformation.setLastPaymentDate(req.getLastPaymentDate());
        bookConformation.setChannel(req.getChannel());
        bookConformation.setReason(req.getReason());
        bookConformation.setSourceType(req.getSourceType());

        // Create BookingRequest from ManualBookingRequest for the booking service
        BookingRequest bookingRequest = buildBookingRequestFromManual(req);

        bookingRequest.setFare(fare);
        // Create the booking
        BookingResponse bookingResponse = bookingService.create(
                bookingRequest, userId, allTravellerIds, bookConformation,
                String.valueOf(bookingPriceInUsd), userId,
                String.valueOf(originalPriceTOUsd), String.valueOf(buyPriceInUsd), String.valueOf(markupPriceInUsd),
                user, exchangeRate, userCurrency);

        // Update transaction with booking source after booking is created
        if (savedTxn != null && bookingResponse.getId() != null) {
            savedTxn.linkSource(TransactionSourceType.BOOKING, bookingResponse.getId());
            savedTxn.setCurrency(bookingResponse.getExchangeCurrency());
            savedTxn.setExchangeRate(Double.valueOf(bookingResponse.getExchangeCurrencyRate()));
            savedTxn.setConvertedAmount(String.valueOf(bookingPrice * Double.parseDouble(bookingResponse.getExchangeCurrencyRate())));
            TransactionAuditHelper.touch(savedTxn, "SYSTEM");
            transactionRepository.save(savedTxn);
        }

        // Save travel information if segments are provided
        if (req.getSegments() != null && !req.getSegments().isEmpty()) {
            com.aerionsoft.application.dto.booking.TravelInformation travelInformation = buildTravelInformationFromSegments(req);
            TravelInformation savedTravelInfo =
                    buildTraveller(travelInformation, bookingResponse.getId(), req.getSegments());

            if (savedTravelInfo != null) {
                com.aerionsoft.application.dto.booking.TravelInformation travelInfoDto = mapEntityToDto(savedTravelInfo);
                bookingResponse.setTravelInformation(travelInfoDto);
            }
        }

        log.info("✅ Manual booking created successfully with ID: " + bookingResponse.getId());

        // Auto-create supplier invoice for CONFIRMED/TICKETED bookings (best-effort; same pattern as import PNR)
        Booking savedBooking = bookingService.getBookingById(bookingResponse.getId());
        createInvoiceForManualBooking(savedBooking, req);

        return bookingResponse;
    }

    @Transactional
    public BookingResponse importConfirmedPnr(ImportPnrRequest req, Long adminUserId, boolean isAdmin) {
        validateImportPnrRequest(req);

        CoreResponse core = req.getCoreResponse();
        String pnr = core.getPnr().trim();
        BookingResponse created = null;


        boolean bookingExists = bookingReadService.existsByPnr(pnr);
        boolean isGroupProvider = Provider.GROUP.name().equals(req.getProviderName());

        if ((!isGroupProvider) && bookingExists) {
            throw ServiceExceptions.duplicate("Booking already exists with PNR: " + pnr);
        }

        Long agencyId = req.getAgencyId();
        String providerNameStr = req.getProviderName() != null ? req.getProviderName().toString() : "DEFAULT";
        String agencyUserCurrency = "USD";
        if (agencyId != null) {
            UserDto agencyUser = userService.getUserById(agencyId);
            if (agencyUser != null && agencyUser.getCurrency() != null && !agencyUser.getCurrency().isBlank()) {
                agencyUserCurrency = agencyUser.getCurrency();
            }
        }

        double exchangeRate = 1.0;
        try {
            exchangeRate = currencyService.getExchangeRateBasedOnUsd(
                    agencyUserCurrency, providerNameStr, req.getChannel());
        } catch (Exception ex) {
            log.warning("Could not get exchange rate for import PNR " + pnr + ": " + ex.getMessage());
        }
        if (exchangeRate <= 0) {
            exchangeRate = 1.0;
        }

        double safeOriginalPrice = req.getOriginalPrice() != null ? req.getOriginalPrice() : req.getBookingPrice();
        double safeBuyPrice = req.getBuyPrice() != null ? req.getBuyPrice() : safeOriginalPrice;
        double safeTaxAmount = req.getTaxAmount() != null ? req.getTaxAmount() : 0.0;

        double reverseMarkup = req.getBookingPrice() - safeOriginalPrice;

        FinalFare fare = new FinalFare();
        String fareCurrency = req.getCurrency() != null ? req.getCurrency() : agencyUserCurrency;
        if (req.getFareDesc() != null && req.getFareDesc().getCurrency() != null && !req.getFareDesc().getCurrency().isBlank()) {
            fareCurrency = req.getFareDesc().getCurrency();
        }
        fare.setBaseFareCurrency(fareCurrency);
        fare.setFareExchangeRate(req.getFareDesc() != null && req.getFareDesc().getFareExchangeRate() != null
                ? req.getFareDesc().getFareExchangeRate()
                : exchangeRate);
        fare.setTax(safeTaxAmount);

        boolean isGroup = Provider.GROUP.name().equals(req.getProviderName().name());

        ManualBookingRequest manualReq = ManualBookingRequest.builder()
                .agencyId(req.getAgencyId())
                .tripType(req.getTripType())
                .providerName(req.getProviderName())
                .bookingClass(req.getBookingClass())
                .type(req.getType())
                .description(req.getDescription())
                .channel(req.getChannel())
                .pnr(pnr)
                .ticketNo(core.getTicketNo())
                .airline(core.getAirline())
                .status(BookingStatus.PNR)
                .bookingPrice((req.getBookingPrice()))
                .originalPrice(safeOriginalPrice)
                .buyPrice(safeBuyPrice)
                .markupAmount(reverseMarkup)
                .taxAmount(safeTaxAmount)
                .currency(req.getCurrency())
                .travellerIds(req.getTravellerIds())
                .itineraries(req.getItineraries())
                .segments(req.getSegments())
                .fare(fare)
                .isBookingAllowed(req.isBookingAllowed())
                .isTicketingAllowed(req.isTicketingAllowed())
                .deductFromWallet(false)
                .reason(req.getReason())
                .timeOffset(req.getTimeOffset())
                .packageBaggageList(req.getPackageBaggageList())
                .lastPaymentDate(core.getLastPaymentDate())
                .sourceType(isGroup ? "GROUP" : "IMPORT")
                .supplierId(req.getSupplierId())
                .build();

        created = createManualBooking(manualReq, adminUserId);

        // Mark booking as imported PNR
        Booking importedBooking = bookingRepository.findById(created.getId()).orElseThrow();
        importedBooking.setImportedPnr(true);
        bookingRepository.save(importedBooking);

        if (created == null || created.getId() == null) {
            throw ServiceExceptions.bookingFailed("Failed to import booking for PNR: " + pnr);
        }

        BookingStatus finalStatus = normalizeImportedStatus(core.getStatus());
        String statusReason = (req.getReason() != null && !req.getReason().isBlank())
                ? req.getReason()
                : "Booking imported from core response";

        bookingService.updateBookingStatus(
                created.getId(),
                finalStatus,
                statusReason,
                core.getTicketNo(),
                isAdmin,
                adminUserId,
                core.getAirlinePnrs()
        );

        if (core.getTickets() != null && !core.getTickets().isEmpty()) {
            Booking booking = bookingService.getBookingById(created.getId());
            persistTravellerTicketNumbers(booking, core.getTickets());
        }

        // Auto-create invoice for the imported PNR (best-effort, should not fail the import)
        try {
            Booking savedBooking = bookingService.getBookingById(created.getId());
            createInvoiceForImportedPnr(savedBooking, req);
        } catch (Exception e) {
            log.warning("Could not auto-create invoice for import PNR " + pnr + ": " + e.getMessage());
        }

        return bookingReadService.mapToResponse(bookingService.getBookingById(created.getId()));
    }

    /**
     * Automatically creates an invoice when a PNR is imported.
     * - Supplier: use req.supplierId when provided, otherwise resolve/create by provider name.
     * - Ledger: find admin ledger by title "Import PNR"; create if not found.
     * - AccountHead: find by title "IMPORT PNR" with ADMIN portal; create if not found.
     * - Invoice + InvoiceItem + SupplierTransactionHistory created for tracking.
     */
    private record BookingInvoiceContext(
            Provider provider,
            String channel,
            Long supplierId,
            double buyPrice,
            double bookingPrice,
            String ticketNo,
            List<TravellerRequest> itineraries,
            List<SegmentRequest> segments,
            String ledgerTitle,
            String accountHeadTitle,
            String invoiceLabel
    ) {}

    private boolean isInvoiceableBookingStatus(BookingStatus status) {
        return status == BookingStatus.CONFIRMED
                || status == BookingStatus.TICKETED
                || status == BookingStatus.TICKET_ISSUED;
    }

    private double parseStoredPrice(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double resolveBookingExchangeRate(Booking booking) {
        double exchangeRate = parseStoredPrice(booking.getBrandCurrency());
        return exchangeRate > 0 ? exchangeRate : 1.0;
    }

    private double toOnlineInvoiceAmount(double usdAmount, Booking booking) {
        return usdAmount * resolveBookingExchangeRate(booking);
    }

    private void tryCreateSupplierInvoice(Booking booking, BookingInvoiceContext context, String issuedTicketNo) {
        try {
            createSupplierInvoice(booking, context);
        } catch (Exception e) {
            log.warning("Could not auto-create invoice for booking " + booking.getId() + ": " + e.getMessage());
            if (shouldDispatchWebhookForTicketedPostProcessFailure(booking, issuedTicketNo)) {
                webhookAlertDispatchService.dispatchTicketedBookingPostProcessFailure(
                        booking,
                        resolveIssuedTicketNo(booking, issuedTicketNo),
                        e.getMessage());
            }
        }
    }

    private boolean shouldDispatchWebhookForTicketedPostProcessFailure(Booking booking, String issuedTicketNo) {
        if (!isInvoiceableBookingStatus(booking.getStatus())) {
            return false;
        }
        String ticketNo = resolveIssuedTicketNo(booking, issuedTicketNo);
        return ticketNo != null && !ticketNo.isBlank();
    }

    private String resolveIssuedTicketNo(Booking booking, String issuedTicketNo) {
        if (issuedTicketNo != null && !issuedTicketNo.isBlank()) {
            return issuedTicketNo.trim();
        }
        if (booking.getTicketNo() != null && !booking.getTicketNo().isBlank()) {
            return booking.getTicketNo().trim();
        }
        return null;
    }

    private String resolveCoreFailureMessage(CoreResponse coreResponse) {
        if (coreResponse == null) {
            return "No response from core system";
        }
        if (coreResponse.getReason() != null && !coreResponse.getReason().isBlank()) {
            return coreResponse.getReason();
        }
        if (coreResponse.getMessage() != null && !coreResponse.getMessage().isBlank()) {
            return coreResponse.getMessage();
        }
        if (coreResponse.getStatus() != null) {
            return "Core status: " + coreResponse.getStatus();
        }
        return "Unknown core error";
    }

    private void dispatchBookingCreateCoreFailureWebhook(
            BookingRequest req,
            User user,
            CoreResponse coreResponse,
            String errorMessage) {
        try {
            String customerName = user != null
                    ? (user.getFullName() != null ? user.getFullName() : user.getEmail())
                    : "Unknown User";
            webhookAlertDispatchService.dispatchBookingCreateCoreFailure(
                    req, customerName, coreResponse, errorMessage);
        } catch (Exception ex) {
            log.warning("Failed to dispatch booking create webhook: " + ex.getMessage());
        }
    }

    private boolean isHoldToBookRepriceResponse(CoreResponse coreResponse) {
        if (coreResponse == null) {
            return false;
        }
        if (coreResponse.getStatus() == BookingStatus.REPRICE) {
            return true;
        }
        return Boolean.TRUE.equals(coreResponse.getIsPriceChanged())
                && coreResponse.getNewPrice() != null
                && !coreResponse.getNewPrice().isBlank();
    }

    private RepricedResponse buildHoldToBookRepriceResponse(Booking booking, CoreResponse coreResponse) {
        RepricedResponse repricedResponse = new RepricedResponse();
        Double markupAmount = Double.parseDouble(booking.getMarkupAmount());
        Double newPrice = Double.valueOf(coreResponse.getNewPrice());
        double newPriceWithMarkup = newPrice + markupAmount;
        double bookedPrice = Double.parseDouble(booking.getBookingPrice());
        if (booking.getExchangeCurrency() != null && booking.getExchangeCurrencyRate() != null) {
            try {
                double exchangeRate = Double.parseDouble(booking.getExchangeCurrencyRate());
                newPriceWithMarkup = newPriceWithMarkup * exchangeRate;
                bookedPrice = bookedPrice * exchangeRate;
            } catch (NumberFormatException e) {
                log.warning("Failed to parse exchange rate for repricing: " + e.getMessage());
            }
        }
        repricedResponse.setAmount(newPriceWithMarkup);
        repricedResponse.setMessage(
                String.format("Price has changed from %.2f to %.2f", bookedPrice, newPriceWithMarkup));
        repricedResponse.setStatus(String.valueOf(BookingStatus.REPRICE));
        repricedResponse.setKey(coreResponse.getTransactionIdentifier());
        return repricedResponse;
    }

    private RepricedResponse handleHoldToBookNonRepriceOutcome(
            Booking booking,
            Long bookingId,
            CoreResponse coreResponse,
            CoreHoldToBooKRequest coreRequest,
            long userId,
            boolean isAdmin) {
        BookingStatus coreStatus = coreResponse.getStatus();
        log.info("Hold-to-book non-reprice outcome: " + coreResponse);

        if (coreStatus == BookingStatus.TICKET_ISSUED
                || coreStatus == BookingStatus.CONFIRMED
                || coreStatus == BookingStatus.TICKETED) {
            bookingService.updateBookingStatus(
                    bookingId,
                    BookingStatus.CONFIRMED,
                    "Booking confirmed successfully",
                    coreResponse.getTicketNo(),
                    isAdmin,
                    userId,
                    null);
            if (coreResponse.getTickets() != null && !coreResponse.getTickets().isEmpty()) {
                persistTravellerTicketNumbers(booking, coreResponse.getTickets());
            }
            Booking confirmedBooking = bookingService.getBookingById(bookingId);
            tryCreateSupplierInvoice(
                    confirmedBooking,
                    buildInvoiceContextFromBooking(confirmedBooking, coreResponse.getTicketNo()),
                    coreResponse.getTicketNo());
            return new RepricedResponse(
                    BookingStatus.CONFIRMED.name(), "Booking confirmed successfully", null, null);
        }

        if (coreStatus == BookingStatus.PROCESS) {
            bookingService.updateStatusOnly(bookingId, BookingStatus.PROCESS);
            String message = coreResponse.getMessage() != null && !coreResponse.getMessage().isBlank()
                    ? coreResponse.getMessage()
                    : "Booking is being processed";
            return new RepricedResponse(BookingStatus.PROCESS.name(), message, null, null);
        }

        return handleHoldToBookFailure(
                bookingId, resolveCoreFailureMessage(coreResponse), coreRequest, userId, null);
    }

    private RepricedResponse handleHoldToBookFailure(
            Long bookingId,
            String errorMessage,
            Object coreRequest,
            long userId,
            Exception exception) {
        log.warning("Hold-to-book failed for booking " + bookingId + ": " + errorMessage);
        try {
            bookingService.updateStatusOnly(bookingId, BookingStatus.PROCESS);
        } catch (Exception statusUpdateEx) {
            log.warning("Failed to update booking status to PROCESS: " + statusUpdateEx.getMessage());
        }
        if (exception != null) {
            errorLogService.logError(
                    "BOOKING_SERVICE",
                    "BOOKING_CREATION_FAILED",
                    errorMessage,
                    coreRequest,
                    null,
                    "500",
                    userId,
                    exception
            );
        }
        holdToBookFailurePublisher.publish(bookingId, errorMessage);
        return new RepricedResponse(BookingStatus.PROCESS.name(), "Booking is being processed", null, null);
    }

    private BookingInvoiceContext buildInvoiceContextFromManual(ManualBookingRequest req, Booking booking) {
        double bookingPriceVal = req.getBookingPrice() != null ? req.getBookingPrice() : 0.0;
        double originalPriceVal = req.getOriginalPrice() != null ? req.getOriginalPrice() : 0.0;
        double buyPriceVal = req.getBuyPrice() != null ? req.getBuyPrice() : originalPriceVal;
        if (buyPriceVal <= 0 && req.getSupplierId() != null && bookingPriceVal > 0) {
            buyPriceVal = bookingPriceVal;
        }
        if (originalPriceVal <= 0 && buyPriceVal > 0) {
            originalPriceVal = buyPriceVal;
        }

        boolean isGroup = req.getProviderName() == Provider.GROUP;
        String ticketNo = resolveInvoiceTicketNo(booking, req.getTicketNo());

        Long supplierId = req.getSupplierId();
        if (supplierId == null) {
            supplierId = supplierResolverService.resolveDefaultAdminSupplier().getId();
        }
        return new BookingInvoiceContext(
                null,
                null,
                supplierId,
                buyPriceVal,
                bookingPriceVal,
                ticketNo,
                req.getItineraries(),
                req.getSegments(),
                isGroup ? "Group Booking" : "Manual Booking",
                isGroup ? "GROUP BOOKING" : "MANUAL BOOKING",
                isGroup ? "Group booking" : "Manual booking"
        );
    }

    private BookingInvoiceContext buildInvoiceContextFromOnline(
            Booking booking, BookingRequest req, String ticketNo, double originalPriceUsd, double buyPriceUsd) {
        Provider provider = req.getProviderName();
        Long supplierId = null;

        double originalPrice = originalPriceUsd > 0
                ? originalPriceUsd
                : parseStoredPrice(booking.getOriginalPrice());
        double buyPrice = buyPriceUsd > 0
                ? buyPriceUsd
                : parseStoredPrice(booking.getBuyPrice());
        if (buyPrice <= 0) {
            buyPrice = originalPrice;
        }
        double bookingPrice = parseStoredPrice(booking.getBookingPrice());

        // Online bookings store USD amounts; invoice payable uses user-currency values.
        buyPrice = toOnlineInvoiceAmount(buyPrice, booking);
        bookingPrice = toOnlineInvoiceAmount(bookingPrice, booking);

        if (provider == Provider.GROUP && req.getResultIndex() != null && !req.getResultIndex().isBlank()) {
            try {
                GroupTicket groupTicket =
                        groupTicketService.getGroupTicketEntity(req.getResultIndex());
                if (groupTicket.getSupplier() != null) {
                    supplierId = groupTicket.getSupplier().getId();
                }
            } catch (Exception e) {
                log.warning("Could not resolve group ticket supplier for GF code " + req.getResultIndex()
                        + ": " + e.getMessage());
            }
        }

        boolean isGroup = provider == Provider.GROUP;
        String channel = req.getChannel() != null ? req.getChannel() : booking.getChannel();
        List<TravellerRequest> itineraries = req.getItineraries() != null && !req.getItineraries().isEmpty()
                ? req.getItineraries()
                : loadTravellerRequestsFromBooking(booking);
        List<SegmentRequest> segments = req.getSegments() != null && !req.getSegments().isEmpty()
                ? req.getSegments()
                : loadSegmentRequestsFromBooking(booking);

        return new BookingInvoiceContext(
                provider,
                channel,
                supplierId,
                buyPrice,
                bookingPrice,
                resolveInvoiceTicketNo(booking, ticketNo),
                itineraries,
                segments,
                isGroup ? "Group Booking" : "Online Booking",
                isGroup ? "GROUP BOOKING" : "ONLINE BOOKING",
                isGroup ? "Group booking" : "Online booking"
        );
    }

    private BookingInvoiceContext buildInvoiceContextFromBooking(Booking booking, String ticketNo) {
        Provider provider = booking.getProviderName();
        Long supplierId = null;

        double buyPrice = parseStoredPrice(booking.getBuyPrice());
        if (buyPrice <= 0) {
            buyPrice = parseStoredPrice(booking.getOriginalPrice());
        }
        double bookingPrice = parseStoredPrice(booking.getBookingPrice());

        // Same as online create: stored USD amounts → user currency via exchange rate.
        buyPrice = toOnlineInvoiceAmount(buyPrice, booking);
        bookingPrice = toOnlineInvoiceAmount(bookingPrice, booking);

        if (provider == Provider.GROUP && booking.getPnr() != null && !booking.getPnr().isBlank()) {
            try {
                GroupTicket groupTicket =
                        groupTicketService.findGroupTicketByGdsPnr(booking.getPnr());
                if (groupTicket != null && groupTicket.getSupplier() != null) {
                    supplierId = groupTicket.getSupplier().getId();
                }
            } catch (Exception e) {
                log.warning("Could not resolve group ticket supplier for PNR " + booking.getPnr()
                        + ": " + e.getMessage());
            }
        }

        String ledgerTitle = "Online Booking";
        String accountHeadTitle = "ONLINE BOOKING";
        String invoiceLabel = "Online booking";
        if (provider == Provider.GROUP) {
            ledgerTitle = "Group Booking";
            accountHeadTitle = "GROUP BOOKING";
            invoiceLabel = "Group booking";
        } else if ("MANUAL".equalsIgnoreCase(booking.getSourceType())) {
            ledgerTitle = "Manual Booking";
            accountHeadTitle = "MANUAL BOOKING";
            invoiceLabel = "Manual booking";
        }

        String effectiveTicketNo = resolveInvoiceTicketNo(booking, ticketNo);
        List<TravellerRequest> itineraries = loadTravellerRequestsFromBooking(booking);
        List<SegmentRequest> segments = loadSegmentRequestsFromBooking(booking);

        return new BookingInvoiceContext(
                provider,
                booking.getChannel(),
                supplierId,
                buyPrice,
                bookingPrice,
                effectiveTicketNo,
                itineraries,
                segments,
                ledgerTitle,
                accountHeadTitle,
                invoiceLabel
        );
    }

    private String resolveInvoiceTicketNo(Booking booking, String contextTicketNo) {
        if (contextTicketNo != null && !contextTicketNo.isBlank()) {
            return contextTicketNo;
        }
        if (booking.getTicketNo() != null && !booking.getTicketNo().isBlank()) {
            return booking.getTicketNo();
        }
        if (booking.getId() == null) {
            return "—";
        }
        String joined = bookingTravellerTicketRepository.findByBookingId(booking.getId()).stream()
                .map(BookingTravellerTicket::getTicketNumber)
                .filter(t -> t != null && !t.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
        return joined.isBlank() ? "—" : joined;
    }

    private List<TravellerRequest> loadTravellerRequestsFromBooking(Booking booking) {
        if (booking.getTravellerIds() == null || booking.getTravellerIds().isBlank()) {
            return Collections.emptyList();
        }
        List<Long> ids = Helper.parseIds(booking.getTravellerIds());
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return travellerService.getTravellersByIds(ids).stream()
                .map(this::toTravellerRequest)
                .toList();
    }

    private TravellerRequest toTravellerRequest(TravellerResponse traveller) {
        TravellerRequest request = new TravellerRequest();
        request.setTitle(traveller.getTitle());
        request.setFirstName(traveller.getFirstName());
        request.setLastName(traveller.getLastName());
        return request;
    }

    private List<SegmentRequest> loadSegmentRequestsFromBooking(Booking booking) {
        if (booking.getId() == null) {
            return Collections.emptyList();
        }
        TravelInformation travelInfo =
                travelInformationRepository.findByBookingId(booking.getId());
        if (travelInfo == null) {
            return Collections.emptyList();
        }

        List<BookingSegment> segments =
                bookingSegmentRepository.findByTravelInformationIdOrderBySegmentOrderAsc(travelInfo.getId());
        if (segments != null && !segments.isEmpty()) {
            return segments.stream().map(this::toSegmentRequest).toList();
        }

        if ((travelInfo.getOrigin() == null || travelInfo.getOrigin().isBlank())
                && (travelInfo.getDestination() == null || travelInfo.getDestination().isBlank())) {
            return Collections.emptyList();
        }

        return List.of(SegmentRequest.builder()
                .origin(SegmentRequest.OriginInfo.builder()
                        .airport(SegmentRequest.AirportInfo.builder()
                                .airportCode(travelInfo.getOrigin())
                                .cityCode(travelInfo.getOrigin())
                                .build())
                        .build())
                .destination(SegmentRequest.DestinationInfo.builder()
                        .airport(SegmentRequest.AirportInfo.builder()
                                .airportCode(travelInfo.getDestination())
                                .cityCode(travelInfo.getDestination())
                                .build())
                        .build())
                .build());
    }

    private SegmentRequest toSegmentRequest(BookingSegment segment) {
        SegmentAirport origin = segmentAirportRepository
                .findFirstBySegmentIdAndAirportTypeOrderByIdAsc(segment.getId(), "ORIGIN")
                .orElse(null);
        SegmentAirport destination = segmentAirportRepository
                .findFirstBySegmentIdAndAirportTypeOrderByIdAsc(segment.getId(), "DESTINATION")
                .orElse(null);

        return SegmentRequest.builder()
                .origin(origin != null ? toSegmentOriginInfo(origin) : null)
                .destination(destination != null ? toSegmentDestinationInfo(destination) : null)
                .segmentOrder(segment.getSegmentOrder())
                .build();
    }

    private SegmentRequest.OriginInfo toSegmentOriginInfo(SegmentAirport airport) {
        return SegmentRequest.OriginInfo.builder()
                .airport(toSegmentAirportInfo(airport))
                .depTime(airport.getTime())
                .build();
    }

    private SegmentRequest.DestinationInfo toSegmentDestinationInfo(SegmentAirport airport) {
        return SegmentRequest.DestinationInfo.builder()
                .airport(toSegmentAirportInfo(airport))
                .arrTime(airport.getTime())
                .build();
    }

    private SegmentRequest.AirportInfo toSegmentAirportInfo(SegmentAirport airport) {
        return SegmentRequest.AirportInfo.builder()
                .airportCode(airport.getAirportCode())
                .airportName(airport.getAirportName())
                .cityCode(airport.getCityCode())
                .cityName(airport.getCityName())
                .build();
    }

    private String resolveSegmentAirportLabel(SegmentRequest.AirportInfo airport) {
        if (airport == null) {
            return null;
        }
        if (airport.getCityCode() != null && !airport.getCityCode().isBlank()) {
            return airport.getCityCode();
        }
        if (airport.getAirportCode() != null && !airport.getAirportCode().isBlank()) {
            return airport.getAirportCode();
        }
        return airport.getCityName();
    }

    private String buildRouteLabel(List<SegmentRequest> segments) {
        if (segments == null || segments.isEmpty()) {
            return null;
        }
        java.util.StringJoiner route = new java.util.StringJoiner(" → ");
        for (SegmentRequest seg : segments) {
            String origin = seg.getOrigin() != null && seg.getOrigin().getAirport() != null
                    ? resolveSegmentAirportLabel(seg.getOrigin().getAirport()) : null;
            String dest = seg.getDestination() != null && seg.getDestination().getAirport() != null
                    ? resolveSegmentAirportLabel(seg.getDestination().getAirport()) : null;
            if (origin != null) {
                route.add(origin);
            }
            if (dest != null && segments.indexOf(seg) == segments.size() - 1) {
                route.add(dest);
            }
        }
        String value = route.toString();
        return value.isBlank() ? null : value;
    }

    private String buildPaxNamesLabel(List<TravellerRequest> itineraries) {
        if (itineraries == null || itineraries.isEmpty()) {
            return null;
        }
        java.util.StringJoiner paxNames = new java.util.StringJoiner(", ");
        for (TravellerRequest traveller : itineraries) {
            String name = ((traveller.getTitle() != null ? traveller.getTitle() + " " : "")
                    + (traveller.getFirstName() != null ? traveller.getFirstName() + " " : "")
                    + (traveller.getLastName() != null ? traveller.getLastName() : "")).trim();
            if (!name.isEmpty()) {
                paxNames.add(name);
            }
        }
        String value = paxNames.toString();
        return value.isBlank() ? null : value;
    }

    private void appendSupplierTransactionDetails(
            Booking booking,
            BookingInvoiceContext context,
            SupplierTransactionHistory txnHistory,
            java.util.List<SupplierTransactionHistoryDetail> details) {

        details.add(SupplierTransactionHistoryDetail.builder()
                .supplierTransactionHistory(txnHistory)
                .key("pnr")
                .value(booking.getPnr() != null ? booking.getPnr() : "—")
                .build());

        String ticketNo = resolveInvoiceTicketNo(booking, context.ticketNo());
        details.add(SupplierTransactionHistoryDetail.builder()
                .supplierTransactionHistory(txnHistory)
                .key("ticketNumber")
                .value(ticketNo)
                .build());

        List<TravellerRequest> itineraries = context.itineraries() != null && !context.itineraries().isEmpty()
                ? context.itineraries()
                : loadTravellerRequestsFromBooking(booking);
        String paxNames = buildPaxNamesLabel(itineraries);
        if (paxNames != null) {
            details.add(SupplierTransactionHistoryDetail.builder()
                    .supplierTransactionHistory(txnHistory)
                    .key("paxNames")
                    .value(paxNames)
                    .build());
        }

        List<SegmentRequest> segments = context.segments() != null && !context.segments().isEmpty()
                ? context.segments()
                : loadSegmentRequestsFromBooking(booking);
        String route = buildRouteLabel(segments);
        if (route != null) {
            details.add(SupplierTransactionHistoryDetail.builder()
                    .supplierTransactionHistory(txnHistory)
                    .key("route")
                    .value(route)
                    .build());
        }
    }

    private double resolveInvoiceBuyPrice(BookingInvoiceContext context) {
        if (context.buyPrice() > 0) {
            return context.buyPrice();
        }
        return context.bookingPrice() > 0 ? context.bookingPrice() : 0.0;
    }

    private double resolveInvoiceSellPrice(BookingInvoiceContext context) {
        if (context.bookingPrice() > 0) {
            return context.bookingPrice();
        }
        return context.buyPrice() > 0 ? context.buyPrice() : 0.0;
    }

    private void createSupplierInvoice(Booking booking, BookingInvoiceContext context) {
        if (!isInvoiceableBookingStatus(booking.getStatus())) {
            log.info("Skipping invoice for booking [" + booking.getId() + "] with status [" + booking.getStatus() + "]");
            return;
        }

        double buyPriceVal = parseStoredPrice(booking.getBuyPrice());
        if (buyPriceVal <= 0) {
            buyPriceVal = parseStoredPrice(booking.getOriginalPrice());
        }
        double sellPriceVal = Double.parseDouble(booking.getBookingPrice());

        double buyPriceToSupplierCurrency = buyPriceVal * booking.getBrandExchangeRate();
        double sellPriceToSupplierCurrency = sellPriceVal * booking.getBrandExchangeRate();

        if (buyPriceVal <= 0) {
            log.info("No invoice buy price for booking [" + booking.getId() + "]; skipping invoice.");
            return;
        }

        String providerName = context.provider() != null ? context.provider().name() : "OTHERS";
        String channel = context.channel() != null ? context.channel() : booking.getChannel();
        LocalDateTime now = UserDateTimeUtil.now();

        Supplier supplier;
        if (context.supplierId() != null) {
            supplier = supplierRepository.findByIdAndAgencyUserIsNull(context.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", context.supplierId()));
        } else if (context.provider() != null) {
            supplier = supplierResolverService.resolveForLiveBooking(context.provider(), channel);
        } else {
            supplier = supplierResolverService.resolveDefaultAdminSupplier();
        }

        Ledger ledger = invoiceLedgerRepository
                .findByTitleAndAgencyIdIsNull(context.ledgerTitle())
                .orElseGet(() -> {
                    Ledger newLedger =
                            Ledger.builder()
                                    .title(context.ledgerTitle())
                                    .description("Auto-created ledger for " + context.invoiceLabel().toLowerCase() + "s")
                                    .agencyId(null)
                                    .createdBy(1L)
                                    .createdAt(now)
                                    .build();
                    return invoiceLedgerRepository.save(newLedger);
                });

        AccountHead accountHead = accountHeadRepository
                .findByAccountHeadTitleAndUsingPortal(context.accountHeadTitle(), UsingPortal.ADMIN)
                .orElseGet(() -> {
                    AccountHead ah = AccountHead.builder()
                            .accountHeadTitle(context.accountHeadTitle())
                            .type(AccountHeadType.EXPENSE)
                            .parentId(0L)
                            .usingPortal(UsingPortal.ADMIN)
                            .createdBy(1L)
                            .updatedBy(1L)
                            .build();
                    return accountHeadRepository.save(ah);
                });

        java.math.BigDecimal buyPrice = java.math.BigDecimal.valueOf(buyPriceToSupplierCurrency);
        java.math.BigDecimal sellPrice = java.math.BigDecimal.valueOf(sellPriceToSupplierCurrency);
        java.math.BigDecimal revenue = sellPrice.subtract(buyPrice);
        String pnrLabel = booking.getPnr() != null && !booking.getPnr().isBlank() ? booking.getPnr() : "—";

        Invoice invoice = Invoice.builder()
                .ledger(ledger)
                .traveller(null)
                .invoiceTitle(context.invoiceLabel() + ": " + pnrLabel)
                .invoiceDetails("Auto-generated invoice for " + context.invoiceLabel().toLowerCase() + " " + booking.getId()
                        + " | PNR: " + pnrLabel + " | Provider: " + providerName
                        + " | Buy: " + buyPrice + " | Sell: " + sellPrice)
                .invoiceDate(java.time.LocalDate.now())
                .paymentMethod("CASH")
                .invoiceAmount(buyPrice)
                .invoiceDiscount(java.math.BigDecimal.ZERO)
                .invoiceServiceCharge(java.math.BigDecimal.ZERO)
                .invoiceRevenue(revenue)
                .status(InvoiceStatus.PENDING)
                .createdBy(1L)
                .createdAt(now)
                .agencyUser(null)
                .build();
        invoice = invoiceRepository.save(invoice);

        InvoiceItem item = InvoiceItem.builder()
                .invoice(invoice)
                .supplier(supplier)
                .accountHead(accountHead)
                .title("PNR - " + pnrLabel)
                .invoiceType(InvoiceType.FLIGHT)
                .quantity(1)
                .sellPrice(sellPrice)
                .buyPrice(buyPrice)
                .step(1)
                .createdBy(1L)
                .createAt(now)
                .build();
        InvoiceItem savedItem = invoiceItemRepository.save(item);

        supplier.setPayableAmount(
                (supplier.getPayableAmount() != null ? supplier.getPayableAmount() : java.math.BigDecimal.ZERO)
                        .add(buyPrice));
        supplierRepository.save(supplier);

        SupplierTransactionHistory txnHistory =
                SupplierTransactionHistory.builder()
                        .invoiceItemId(savedItem.getId())
                        .invoiceId(invoice.getId())
                        .agencyId(null)
                        .ledgerId(ledger.getId())
                        .supplierId(supplier.getId())
                        .payableAmount(buyPrice)
                        .title(context.invoiceLabel() + ": " + pnrLabel)
                        .description("Auto-created for " + context.invoiceLabel().toLowerCase() + " id " + booking.getId()
                                + " | Provider: " + providerName)
                        .createdDate(now)
                        .build();

        java.util.List<SupplierTransactionHistoryDetail> details =
                new java.util.ArrayList<>();
        appendSupplierTransactionDetails(booking, context, txnHistory, details);

        txnHistory.setDetails(details);
        supplierTransactionHistoryRepository.save(txnHistory);

        log.info("📄 Auto-created invoice [" + invoice.getId() + "] for booking [" + booking.getId()
                + "] with supplier [" + supplier.getName() + "]");
    }

    private void createInvoiceForImportedPnr(Booking booking, ImportPnrRequest req) {
        double originalPrice = req.getOriginalPrice() != null ? req.getOriginalPrice() : 0.0;
        double buyPrice = req.getBuyPrice() != null ? req.getBuyPrice() : originalPrice;
        double bookingPrice = req.getBookingPrice() != null ? req.getBookingPrice() : 0.0;
        if (buyPrice <= 0 && bookingPrice <= 0) {
            log.info("Booking price is zero for import PNR [" + booking.getPnr() + "]; skipping invoice creation.");
            return;
        }

        BookingInvoiceContext context = new BookingInvoiceContext(
                req.getProviderName(),
                req.getChannel() != null ? req.getChannel() : booking.getChannel(),
                req.getSupplierId(),
                buyPrice,
                bookingPrice,
                booking.getTicketNo(),
                req.getItineraries(),
                req.getSegments(),
                "Import PNR",
                "IMPORT PNR",
                "Import PNR"
        );
        createSupplierInvoice(booking, context);
    }

    /**
     * Automatically creates a payable invoice after a manual booking is saved.
     * Mirrors {@link #createInvoiceForImportedPnr}: supplier from {@code req.supplierId} when set,
     * otherwise resolve/create by channel name (e.g. galileo-bd → GALILEO_BD); ledger "Manual Booking"; account head "MANUAL BOOKING".
     */
    private void createInvoiceForManualBooking(Booking booking, ManualBookingRequest req) {
        BookingStatus invoiceStatus = req.getStatus() != null ? req.getStatus() : booking.getStatus();
        if (!isInvoiceableBookingStatus(invoiceStatus)) {
            log.info("Skipping invoice for manual booking [" + booking.getId()
                    + "] with status [" + invoiceStatus + "]; invoice only created for CONFIRMED/TICKETED/TICKET_ISSUED.");
            return;
        }

        try {
            createSupplierInvoice(booking, buildInvoiceContextFromManual(req, booking));
        } catch (Exception e) {
            log.warning("Could not auto-create invoice for manual booking "
                    + booking.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Creates a manual booking from a group ticket.
     * Segment information is automatically fetched from the GroupTicket entity's flightInfo.
     * Works similarly to importConfirmedPnr but derives segments, PNR, airline info from the GroupTicket.
     */
    @Transactional
    public BookingResponse groupTicketManualBooking(GroupTicketManualRequest req, Long adminUserId, boolean isAdmin) {
        // 1. Fetch the GroupTicket by gfCode
        String gfCode = req.getGfCode().trim();
        GroupTicket groupTicket =
                groupTicketService.getGroupTicketEntity(gfCode);

        if (groupTicket == null) {
            throw ServiceExceptions.notFound("Group ticket not found for GF code: " + gfCode);
        }

        String pnr = groupTicket.getGdsPnr();
        if (pnr == null || pnr.isBlank()) {
            throw ServiceExceptions.bookingFailed("Group ticket does not have a GDS PNR");
        }

        // 2. Convert travelers from request to TravellerRequest list
        List<TravellerRequest> itineraries = req.getTravelers().stream()
                .map(t -> {
                    TravellerRequest tr = new TravellerRequest();
                    tr.setTitle(t.getTitle());
                    tr.setFirstName(t.getFirstName());
                    tr.setLastName(t.getLastName());
                    tr.setGender(Gender.valueOf(t.getGender().toUpperCase()));
                    tr.setDob(t.getDob());
                    tr.setPassportNo(t.getPassportNo() != null ? t.getPassportNo() : "");
                    tr.setPassportIssueDate(t.getPassportIssueDate() != null ? t.getPassportIssueDate() : "2020-01-01");
                    tr.setPassportExpiryDate(t.getPassportExpiryDate() != null ? t.getPassportExpiryDate() : "2030-01-01");
                    tr.setNationality(t.getNationality() != null ? t.getNationality() : "");
                    tr.setMobile("0000000000");
                    tr.setMobileCountryCode(t.getMobileCountryCode() != null ? t.getMobileCountryCode() : "880");
                    tr.setCountryName(t.getNationality() != null ? t.getNationality() : "Bangladesh");
                    tr.setCountryCode("BD");
                    tr.setCityName("Dhaka");
                    tr.setCityCode("DAC");
                    tr.setMealCode("NONE");
                    tr.setAddressLine1("N/A");
                    tr.setAddressLine2("");
                    tr.setPassportImageUrl(t.getPassportImageUrl() != null ? t.getPassportImageUrl() : "");
                    tr.setVisaOrNidImageURL(t.getVisaOrNidImageURL() != null ? t.getVisaOrNidImageURL() : "");
                    return tr;
                })
                .toList();

        // 3. Convert FlightInfo from GroupTicket to SegmentRequest list
        List<SegmentRequest> segments = buildSegmentsFromGroupTicket(groupTicket);

        // 4. Build FinalFare from the request fare
        GroupTicketManualRequest.GroupTicketFare reqFare = req.getFare();
        FinalFare fare = new FinalFare();
        fare.setCurrency(reqFare.getCurrency());
        fare.setBaseFare(reqFare.getBaseFare());
        fare.setTax(reqFare.getTax());
        fare.setOfferFare(reqFare.getTotalFare());
        fare.setOtherCharges(0.0);
        fare.setDiscount(0.0);
        fare.setPublishedFare(reqFare.getTotalFare());
        fare.setTotalMealCharges(0.0);
        fare.setBaseFareCurrency(reqFare.getCurrency());
        fare.setRemarks("group-ticket-manual");
        fare.setFareExchangeRate(1.0);
        fare.setAit(0.0);

        // 5. Determine trip type from group ticket's flightType
        TripType tripType = resolveTripTypeFromFlightType(groupTicket.getFlightType());

        // 6. Build ManualBookingRequest
        Long agencyId = req.getAgencyId() != null ? req.getAgencyId() : adminUserId;
        double bookingTotalPrice = reqFare.getTotalFare();
        double buyPrice = groupTicket.getCosting() != null && groupTicket.getCosting() > 0
                ? groupTicket.getCosting()
                : bookingTotalPrice;
        Long groupSupplierId = groupTicket.getSupplier() != null ? groupTicket.getSupplier().getId() : null;

        ManualBookingRequest manualReq = ManualBookingRequest.builder()
                .agencyId(agencyId)
                .tripType(tripType)
                .providerName(Provider.GROUP)
                .bookingClass(BookingClass.ECONOMY)
                .type(BookingType.FLIGHT)
                .description("Group Ticket Booking - " + groupTicket.getTitle())
                .channel("GROUP")
                .pnr(pnr)
                .ticketNo("")
                .airline(groupTicket.getAirlineCode())
                .status(BookingStatus.CONFIRMED)
                .bookingPrice(bookingTotalPrice)
                .originalPrice(buyPrice)
                .buyPrice(buyPrice)
                .markupAmount(Math.max(0.0, bookingTotalPrice - buyPrice))
                .taxAmount(reqFare.getTax())
                .currency(reqFare.getCurrency())
                .itineraries(itineraries)
                .segments(segments)
                .fare(fare)
                .isBookingAllowed(true)
                .isTicketingAllowed(true)
                .deductFromWallet(true)
                .supplierId(groupSupplierId)
                .sourceType("GROUP")
                .groupTicketType(groupTicket.getTicketType())
                .reason("Group ticket manual booking for GF: " + gfCode)
                .build();

        // 7. Create the manual booking
        BookingResponse created = createManualBooking(manualReq, adminUserId);

        // 8. Update booking status to CONFIRMED
        bookingService.updateBookingStatus(
                created.getId(),
                BookingStatus.CONFIRMED,
                "Group ticket manual booking confirmed for GF: " + gfCode,
                "",
                isAdmin,
                adminUserId,
                groupTicket.getAirlinePnr()
        );

        log.info("✅ Group ticket manual booking created successfully with ID: " + created.getId() + " for GF: " + gfCode);

        return bookingReadService.mapToResponse(bookingService.getBookingById(created.getId()));
    }

    /**
     * Converts FlightInfo list from a GroupTicket entity into SegmentRequest list
     */
    private List<SegmentRequest> buildSegmentsFromGroupTicket(GroupTicket groupTicket) {
        List<FlightInfo> flightInfoList = groupTicket.getFlightInfo();

        if (flightInfoList == null || flightInfoList.isEmpty()) {
            // Fallback: build a single segment from the GroupTicket itself
            SegmentRequest segment = SegmentRequest.builder()
                    .origin(SegmentRequest.OriginInfo.builder()
                            .airport(SegmentRequest.AirportInfo.builder()
                                    .airportCode(groupTicket.getOrigin())
                                    .airportName(groupTicket.getOrigin())
                                    .build())
                            .depTime(groupTicket.getDepartureDate() + "T" + (groupTicket.getDepartureTime() != null ? groupTicket.getDepartureTime() : "00:00"))
                            .build())
                    .destination(SegmentRequest.DestinationInfo.builder()
                            .airport(SegmentRequest.AirportInfo.builder()
                                    .airportCode(groupTicket.getDestination())
                                    .airportName(groupTicket.getDestination())
                                    .build())
                            .arrTime(groupTicket.getArrivalDate() + "T" + (groupTicket.getArrivalTime() != null ? groupTicket.getArrivalTime() : "00:00"))
                            .build())
                    .airline(SegmentRequest.AirlineInfo.builder()
                            .airlineCode(groupTicket.getAirlineCode())
                            .airlineName(groupTicket.getAirlineName())
                            .build())
                    .segmentOrder(0)
                    .segmentType("ONEWAY")
                    .build();

            return List.of(segment);
        }

        List<SegmentRequest> segments = new ArrayList<>();
        int order = 0;

        for (FlightInfo fi : flightInfoList) {
            SegmentRequest segment = SegmentRequest.builder()
                    .origin(SegmentRequest.OriginInfo.builder()
                            .airport(SegmentRequest.AirportInfo.builder()
                                    .airportCode(fi.getOrigin())
                                    .airportName(fi.getOrigin())
                                    .terminal(fi.getOriginTerminal())
                                    .build())
                            .depTime(fi.getDepartureDate() + "T" + (fi.getDepartureTime() != null ? fi.getDepartureTime() : "00:00"))
                            .build())
                    .destination(SegmentRequest.DestinationInfo.builder()
                            .airport(SegmentRequest.AirportInfo.builder()
                                    .airportCode(fi.getDestination())
                                    .airportName(fi.getDestination())
                                    .terminal(fi.getDestinationTerminal())
                                    .build())
                            .arrTime(fi.getArrivalDate() + "T" + (fi.getArrivalTime() != null ? fi.getArrivalTime() : "00:00"))
                            .build())
                    .airline(SegmentRequest.AirlineInfo.builder()
                            .airlineCode(groupTicket.getAirlineCode())
                            .airlineName(groupTicket.getAirlineName())
                            .flightNumber(fi.getFlightNumber())
                            .build())
                    .duration(fi.getDurationInMinutes())
                    .segmentOrder(order++)
                    .segmentType(resolveSegmentType(fi.getSegmentType()))
                    .build();

            segments.add(segment);
        }

        return segments;
    }

    private String resolveSegmentType(String segmentType) {
        if (segmentType == null || segmentType.isBlank()) {
            return "ONEWAY";
        }
        String normalized = segmentType.trim().toUpperCase();
        return "RETURN".equals(normalized) ? "RETURN" : "ONEWAY";
    }

    private TripType resolveTripTypeFromFlightType(String flightType) {
        if (flightType == null || flightType.isBlank()) {
            return TripType.ONE_WAY;
        }

        String normalized = flightType.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return switch (normalized) {
            case "ONE_WAY", "ONEWAY" -> TripType.ONE_WAY;
            case "ROUND_TRIP", "ROUNDTRIP" -> TripType.ROUND_TRIP;
            case "MULTI_CITY", "MULTICITY" -> TripType.MULTI_CITY;
            default -> {
                try {
                    yield TripType.fromValue(normalized);
                } catch (IllegalArgumentException e) {
                    yield TripType.ONE_WAY;
                }
            }
        };
    }

    private void validateImportPnrRequest(ImportPnrRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.getCoreResponse() == null) {
            errors.add("Core response is required");
        } else {
            if (req.getCoreResponse().getPnr() == null || req.getCoreResponse().getPnr().isBlank()) {
                errors.add("Core response PNR is required");
            }
            if (req.getCoreResponse().getTicketNo() == null || req.getCoreResponse().getTicketNo().isBlank()) {
                errors.add("Core response ticketNo is required");
            }
            if (req.getCoreResponse().getAirline() == null || req.getCoreResponse().getAirline().isBlank()) {
                errors.add("Core response airline is required");
            }
            if (req.getCoreResponse().getStatus() == null) {
                errors.add("Core response status is required");
            }
        }

        if ((req.getItineraries() == null || req.getItineraries().isEmpty()) &&
                (req.getTravellerIds() == null || req.getTravellerIds().isEmpty())) {
            errors.add("At least one traveller (new or existing) is required");
        }

        if (!errors.isEmpty()) {
            throw ServiceExceptions.validation("Import PNR validation failed: " + String.join(", ", errors));
        }
    }

    private BookingStatus normalizeImportedStatus(BookingStatus status) {
        if (status == null) {
            return BookingStatus.CONFIRMED;
        }

        if (status == BookingStatus.CONFIRMED || status == BookingStatus.TICKETED || status == BookingStatus.TICKET_ISSUED) {
            return status;
        }

        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Imported core response must be CONFIRMED, TICKETED, or TICKET_ISSUED");
    }

    /**
     * Validates manual booking request fields
     */
    private void validateManualBookingRequest(ManualBookingRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.getBookingClass() == null) {
            errors.add("Booking class is required");
        }

        if (req.getTripType() == null) {
            errors.add("Trip type is required");
        }

        if (req.getAirline() == null || req.getAirline().isBlank()) {
            errors.add("Airline is required");
        }

        if (req.getStatus() == null) {
            errors.add("Booking status is required");
        }

        if (req.getBookingPrice() == null || req.getBookingPrice() <= 0) {
            errors.add("Valid booking price is required");
        }

        // For TICKETED status, ticket number is required
        if (req.getStatus() == BookingStatus.TICKETED &&
                (req.getTicketNo() == null || req.getTicketNo().isBlank())) {
            errors.add("Ticket number is required for TICKETED status");
        }

        // For CONFIRMED or TICKETED status, PNR is typically required
        if ((req.getStatus() == BookingStatus.CONFIRMED || req.getStatus() == BookingStatus.TICKETED) &&
                (req.getPnr() == null || req.getPnr().isBlank())) {
            errors.add("PNR is required for CONFIRMED or TICKETED status");
        }

        if ((req.getItineraries() == null || req.getItineraries().isEmpty()) &&
                (req.getTravellerIds() == null || req.getTravellerIds().isEmpty())) {
            errors.add("At least one traveller (new or existing) is required");
        }

        if (!errors.isEmpty()) {
            throw ServiceExceptions.validation("Manual booking validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Converts ManualBookingRequest to BookingRequest for service compatibility
     */
    private BookingRequest buildBookingRequestFromManual(ManualBookingRequest req) {
        FinalFare fare = req.getFare() != null ? req.getFare() : buildDefaultFare(req);

        return BookingRequest.builder()
                .tripType(req.getTripType())
                .bookType(BookType.MANUAL)
                .providerName(req.getProviderName() != null ? req.getProviderName() : Provider.OTHERS)
                .bookingClass(req.getBookingClass())
                .type(req.getType())
                .description(req.getDescription())
                .channel(req.getChannel())
                .travellerIds(req.getTravellerIds())
                .itineraries(req.getItineraries())
                .fare(fare)
                .isBookingAllowed(req.isBookingAllowed())
                .isTicketingAllowed(req.isTicketingAllowed())
                .segments(req.getSegments())
                .timeOffset(req.getTimeOffset())
                .packageBaggageList(req.getPackageBaggageList())
                .groupTicketType(req.getGroupTicketType())
                .build();
    }

    private FinalFare buildDefaultFare(ManualBookingRequest req) {
        FinalFare fare = new FinalFare();
        String currency = req.getCurrency() != null && !req.getCurrency().isBlank() ? req.getCurrency() : "USD";
        Double bookingPrice = req.getBookingPrice() != null ? req.getBookingPrice() : 0.0;
        Double taxAmount = req.getTaxAmount() != null ? req.getTaxAmount() : 0.0;
        Double baseFare = Math.max(0.0, bookingPrice - taxAmount);

        fare.setCurrency(currency);
        fare.setBaseFare(baseFare);
        fare.setTax(taxAmount);
        fare.setOfferFare(bookingPrice);
        fare.setOtherCharges(0.0);
        fare.setDiscount(0.0);
        fare.setPublishedFare(bookingPrice);
        fare.setTotalMealCharges(0.0);
        fare.setBaseFareCurrency(currency);
        fare.setRemarks("manual/imported");
        fare.setFareExchangeRate(1.0);
        fare.setAit(0.0);
        return fare;
    }

    /**
     * Builds TravelInformation from manual booking segments
     */
    private com.aerionsoft.application.dto.booking.TravelInformation buildTravelInformationFromSegments(ManualBookingRequest req) {
        com.aerionsoft.application.dto.booking.TravelInformation travelInfo = new com.aerionsoft.application.dto.booking.TravelInformation();

        if (req.getSegments() != null && !req.getSegments().isEmpty()) {
            SegmentRequest firstSegment = req.getSegments().get(0);

            if (firstSegment.getAirline() != null) {
                travelInfo.setAirlineName(firstSegment.getAirline().getAirlineName());
                travelInfo.setAirlineCode(firstSegment.getAirline().getAirlineCode());
                travelInfo.setFlightNumber(firstSegment.getAirline().getFlightNumber());
            }

            if (firstSegment.getOrigin() != null && firstSegment.getOrigin().getAirport() != null) {
                travelInfo.setOrigin(firstSegment.getOrigin().getAirport().getAirportCode());
                travelInfo.setDepartureAirport(firstSegment.getOrigin().getAirport().getAirportName());
                travelInfo.setDepartureTime(firstSegment.getOrigin().getDepTime());
                // Extract date from depTime (e.g. "2026-04-21T13:55" -> "2026-04-21")
                if (firstSegment.getOrigin().getDepTime() != null && firstSegment.getOrigin().getDepTime().contains("T")) {
                    travelInfo.setDepartureDate(firstSegment.getOrigin().getDepTime().split("T")[0]);
                }
            }

            if (firstSegment.getDestination() != null && firstSegment.getDestination().getAirport() != null) {
                travelInfo.setDestination(firstSegment.getDestination().getAirport().getAirportCode());
                travelInfo.setArrivalAirport(firstSegment.getDestination().getAirport().getAirportName());
                travelInfo.setArrivalTime(firstSegment.getDestination().getArrTime());
                // Extract date from arrTime (e.g. "2026-04-22T06:30" -> "2026-04-22")
                if (firstSegment.getDestination().getArrTime() != null && firstSegment.getDestination().getArrTime().contains("T")) {
                    travelInfo.setArrivalDate(firstSegment.getDestination().getArrTime().split("T")[0]);
                }
            }

            travelInfo.setDuration(firstSegment.getDuration() != null ? String.valueOf(firstSegment.getDuration()) : null);

            // Parse baggage as double if possible
            if (firstSegment.getBaggage() != null) {
                try {
                    travelInfo.setBaggageKg(Double.parseDouble(firstSegment.getBaggage().replaceAll("[^0-9.]", "")));
                } catch (NumberFormatException e) {
                    // If not parseable, set as null
                    travelInfo.setBaggageKg(null);
                }
            }
        }

        travelInfo.setCurrency(req.getCurrency());
        travelInfo.setBaseFare(req.getBookingPrice());
        travelInfo.setTax(req.getTaxAmount());
        travelInfo.setTicketNumber(req.getTicketNo());

        return travelInfo;
    }


    public boolean cancelByPnr(String pnr, String channel, String reason) {

        Booking booking = bookingService.bookingByPnr(pnr);

        if (booking == null) {
            throw ServiceExceptions.notFound("Booking not found with PNR: " + pnr);
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ServiceExceptions.duplicate("Booking is already cancelled with PNR: " + pnr);
        }

        CoreCancelBookRequest cancelRequest = new CoreCancelBookRequest();
        cancelRequest.setConfirmationId(pnr);
        cancelRequest.setChannel(channel);

        CoreCancelBookingStatusResponseDto response = callCoreApi(cancelRequest, coreCancelBookingEndpoint, CoreCancelBookingStatusResponseDto.class);

        if (response.isCancelled()) {
            bookingService.updateBookingStatus(booking.getId(), BookingStatus.CANCELLED, reason, null);
        } else {
            throw ServiceExceptions.bookingFailed("Failed to cancel booking: " + response.getMessage());
        }
        return true;
    }


    @Transactional
    public RepricedResponse holdToBook(Long id, long userId, boolean isAdmin) {
        Optional<Booking> booking = Optional.ofNullable(bookingService.getBookingById(id));


        if (booking.isEmpty()) {
            throw new ResourceNotFoundException("Booking", id);
        }


        long userIdtemp = isAdmin ? booking.get().getCreatedBy().getId() : userId;
        User user = userService.getUser(userIdtemp);
        if (user == null) {
            throw new ResourceNotFoundException("User", userIdtemp);
        }

        Booking b = booking.get();
        if (b.getStatus() != BookingStatus.PNR) {
            throw ServiceExceptions.invalidState("Booking is not in ON_HOLD status");
        }

        double bookingPrice = Double.parseDouble(booking.get().getBookingPrice());
        Double convertedBookingPrice = bookingPrice * (booking.get().getExchangeCurrencyRate() != null ? Double.parseDouble(booking.get().getExchangeCurrencyRate()) : 1.0);
        Double userBalance = user.getBalance() != null ? user.getBalance() : 0.0;

        if (isAdmin) {
            boolean canOverrideBalance = hasAdminOverrideBalancePermission(userId);
            if (!canOverrideBalance) {
                boolean hasEnoughBalance = creditLimitValidatorService.hasSufficientBalance(
                        userIdtemp, userBalance, convertedBookingPrice);
                if (!hasEnoughBalance) {
                    double availableBalance = creditLimitValidatorService.getAvailableBalance(userIdtemp, userBalance);
                    throw ServiceExceptions.insufficientBalance("Insufficient balance to convert hold to book. Required: " +
                            Helper.formatMoney(convertedBookingPrice) + ", Available: " + Helper.formatMoney(availableBalance));
                }
            }
        } else {
            // Non-admin: check balance; no auto-grant allowed
            boolean hasEnoughBalance = userBalance >= convertedBookingPrice;

            // Check credit limit - allows wallet to go negative up to creditLimit
            if (!hasEnoughBalance) {
                hasEnoughBalance = creditLimitValidatorService.hasSufficientBalance(
                        userId, userBalance, convertedBookingPrice);
            }

            if (!hasEnoughBalance) {
                double availableBalance = creditLimitValidatorService.getAvailableBalance(userId, userBalance);
                log.info("💳 User " + userId + " has insufficient balance. Available: " + Helper.formatMoney(availableBalance) +
                        ", Required: " + Helper.formatMoney(convertedBookingPrice));
                throw ServiceExceptions.insufficientBalance("Insufficient balance to convert hold to book. Required: " +
                        Helper.formatMoney(convertedBookingPrice) + ", Available: " + Helper.formatMoney(availableBalance));
            }
        }

        if (!b.isTicketingAllowed()) {
            bookingService.updateStatusOnly(id, BookingStatus.PROCESS);
            return new RepricedResponse(BookingStatus.PROCESS.name(), "Booking is being processed", null, null);
        }

        CoreHoldToBooKRequest coreRequest = buildCoreRequestForHoldToBook(b);

        try {
            CoreResponse coreResponse = callCoreApi(coreRequest, coreHoldToBookEndpoint, CoreResponse.class);

            if (coreResponse == null) {
                return handleHoldToBookFailure(id, "Failed to get response from core system for hold-to-book",
                        coreRequest, userId, null);
            }
            if (isHoldToBookRepriceResponse(coreResponse)) {
                return buildHoldToBookRepriceResponse(b, coreResponse);
            }

            return handleHoldToBookNonRepriceOutcome(b, id, coreResponse, coreRequest, userId, isAdmin);

        } catch (MicroserviceException e) {
            log.warning("MicroserviceException during hold-to-book: " + e.getMessage());
            return handleHoldToBookFailure(id, e.getMessage(), coreRequest, userId, null);
        } catch (Exception e) {
            log.severe("Error during hold-to-book: " + e.getMessage());
            return handleHoldToBookFailure(id, e.getMessage(), coreRequest, userId, e);
        }
    }

    public boolean repriceConfirmation(RepriceConfirmationRequest request, long userId, boolean isAdmin) {
        RepricingRequest coreRequest = new RepricingRequest();
        coreRequest.setKey(request.getTransactionId());
        coreRequest.setChannel(request.getChannel());
        coreRequest.setConfirmed(true);

        try {
            CoreResponse coreResponse = callCoreApi(coreRequest, coreRepricingEndpoint, CoreResponse.class);

            if (coreResponse == null) {
                throw ServiceExceptions.bookingFailed("Failed to get response from core system for reprice confirmation");
            }

            Booking booking = bookingService.bookingByPnr(request.getPnr());
            if (booking == null) {
                throw ServiceExceptions.notFound("Booking not found with PNR: " + request.getPnr());
            }

            if (isHoldToBookRepriceResponse(coreResponse)) {
                log.info("Reprice confirmation returned another reprice for PNR: " + request.getPnr());
                return false;
            }

            log.info("Reprice confirmation response: " + coreResponse);

            if (coreResponse.getStatus() == BookingStatus.TICKET_ISSUED
                    || coreResponse.getStatus() == BookingStatus.CONFIRMED
                    || coreResponse.getStatus() == BookingStatus.TICKETED) {
                bookingService.updateBookingStatus(
                        booking.getId(),
                        BookingStatus.CONFIRMED,
                        "Booking confirmed after reprice",
                        coreResponse.getTicketNo(),
                        isAdmin,
                        userId,
                        coreResponse.getAirlinePnrs());
                if (coreResponse.getTickets() != null && !coreResponse.getTickets().isEmpty()) {
                    persistTravellerTicketNumbers(booking, coreResponse.getTickets());
                }
                Booking confirmedBooking = bookingService.getBookingById(booking.getId());
                tryCreateSupplierInvoice(
                        confirmedBooking,
                        buildInvoiceContextFromBooking(confirmedBooking, coreResponse.getTicketNo()),
                        coreResponse.getTicketNo());
                return true;
            }

            if (coreResponse.getStatus() == BookingStatus.PROCESS) {
                bookingService.updateStatusOnly(booking.getId(), BookingStatus.PROCESS);
                return true;
            }

            handleHoldToBookFailure(
                    booking.getId(),
                    resolveCoreFailureMessage(coreResponse),
                    coreRequest,
                    userId,
                    null);
            return true;

        } catch (MicroserviceException e) {
            log.warning("MicroserviceException during reprice confirmation: " + e.getMessage());
            Booking booking = bookingService.bookingByPnr(request.getPnr());
            if (booking != null) {
                handleHoldToBookFailure(booking.getId(), e.getMessage(), coreRequest, userId, null);
                return true;
            }
            throw e;
        } catch (Exception e) {
            log.severe("Error during reprice confirmation: " + e.getMessage());

            errorLogService.logError(
                    "BOOKING_SERVICE",
                    "REPRICE_CONFIRMATION_FAILED",
                    e.getMessage(),
                    coreRequest,
                    null,
                    "500",
                    userId,
                    e
            );

            Booking booking = bookingService.bookingByPnr(request.getPnr());
            if (booking != null) {
                handleHoldToBookFailure(booking.getId(), e.getMessage(), coreRequest, userId, e);
                return true;
            }

            throw ServiceExceptions.bookingFailed("Failed to confirm reprice in core system: " + e.getMessage());
        }
    }

    private boolean hasAdminOverrideBalancePermission(Long adminUserId) {
        if (adminUserId == null) {
            return false;
        }
        return roleAssignmentRepository.findByEntityTypeAndEntityId("ADMIN", adminUserId)
                .map(ra -> ra.getRole() != null && ra.getRole().getPermissions() != null
                        && ra.getRole().getPermissions().stream()
                        .anyMatch(p -> p.getSlug() != null && "override-balance".equalsIgnoreCase(p.getSlug())))
                .orElse(false);
    }

}
