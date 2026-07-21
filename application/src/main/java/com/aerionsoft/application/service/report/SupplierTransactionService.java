package com.aerionsoft.application.service.report;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.CreatorDto;
import com.aerionsoft.application.dto.business.BusinessDto;
import com.aerionsoft.application.dto.client.invoice.response.LedgerShortDTO;
import com.aerionsoft.application.dto.client.invoice.response.SupplierTransactionHistoryDTO;
import com.aerionsoft.application.dto.client.invoice.response.SupplierTransactionHistoryDetailDTO;
import com.aerionsoft.application.dto.client.invoice.response.SupplierTransactionHistoryWithTotalDTO;
import com.aerionsoft.application.dto.report.SupplierTransactionCreateDTO;
import com.aerionsoft.application.dto.report.SupplierTransactionHistorySpecification;
import com.aerionsoft.application.dto.report.SupplierTransactionReportRowDTO;
import com.aerionsoft.application.dto.report.SupplierTransactionReportSummaryDTO;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.client.InvoiceItem;
import com.aerionsoft.application.entity.client.Ledger;
import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.entity.client.SupplierTransactionHistory;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.group.TravelInformation;
import com.aerionsoft.application.repository.booking.TravelInformationRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.client.InvoiceItemRepository;
import com.aerionsoft.application.repository.client.InvoiceLedgerRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.repository.client.SupplierTransactionHistoryRepository;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.service.wallet.BankLedgerService;
import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.entity.wallet.DepositBank;
import com.aerionsoft.application.enums.client.ManualInvoicePaymentType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupplierTransactionService {
    @Autowired
    private SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private InvoiceLedgerRepository ledgerRepository;
    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TravelInformationRepository travelInformationRepository;

    @Autowired
    private BankLedgerService bankLedgerService;

    @Autowired
    private TimestampMapper timestampMapper;

    @Transactional
    public SupplierTransactionHistoryWithTotalDTO getTransactionHistories(
            String provider,
            Long authUserId,
            int page,
            int size,
            Long supplierId,
            Long ledgerId,
            LocalDate from,
            LocalDate to
    ) {

        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        Long agencyId = null;

        if (!isAdmin) {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));

            agencyId = user.getParentUser() != null
                    ? user.getParentUser().getId()
                    : user.getId();
        }

        Specification<SupplierTransactionHistory> spec = SupplierTransactionHistorySpecification.hasAgencyId(agencyId)
                .and(SupplierTransactionHistorySpecification.hasSupplierId(supplierId))
                .and(SupplierTransactionHistorySpecification.hasLedgerId(ledgerId))
                .and(SupplierTransactionHistorySpecification.createdInUserRange(from, to));

        BigDecimal totalPayableAmount = null;
        BigDecimal totalPaidAmount = null;
        BigDecimal initialBalance = BigDecimal.ZERO;

        if (supplierId != null) {
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
            totalPayableAmount = supplier.getPayableAmount();
            totalPaidAmount = supplier.getPaidAmount();
            initialBalance = nullToZero(supplier.getInitialBalance());
        }

        List<SupplierTransactionHistory> allHistories = supplierTransactionHistoryRepository.findAll(
                spec, Sort.by(Sort.Direction.ASC, "createdDate"));
        initializeDetails(allHistories);

        Map<Long, InvoiceItem> invoiceItemMap = loadInvoiceItems(allHistories);
        Map<String, BookingContext> bookingContextMap = loadBookingContexts(allHistories);
        Map<Long, BigDecimal> outstandingById = computeOutstandingBalances(allHistories, invoiceItemMap, initialBalance);

        SupplierTransactionReportSummaryDTO summary = buildSummary(
                allHistories, invoiceItemMap, totalPayableAmount, totalPaidAmount, initialBalance);

        Page<SupplierTransactionHistory> histories = supplierTransactionHistoryRepository.findAll(spec, pageable);
        initializeDetails(histories.getContent());

        Page<SupplierTransactionReportRowDTO> reportRows = histories.map(entity ->
                toReportRow(entity, invoiceItemMap, bookingContextMap, outstandingById));

        SupplierTransactionHistoryWithTotalDTO response = new SupplierTransactionHistoryWithTotalDTO();

        response.setTotalPayable(totalPayableAmount);
        response.setTotalPaidAmount(totalPaidAmount);
        response.setSummary(summary);
        response.setSupplierTransactionHistoryWithTotalDTO(reportRows);

        return response;
    }

    private Map<Long, InvoiceItem> loadInvoiceItems(List<SupplierTransactionHistory> histories) {
        Set<Long> itemIds = histories.stream()
                .map(SupplierTransactionHistory::getInvoiceItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (itemIds.isEmpty()) {
            return Map.of();
        }

        return invoiceItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(InvoiceItem::getId, item -> item));
    }

    private Map<String, BookingContext> loadBookingContexts(List<SupplierTransactionHistory> histories) {
        Set<String> pnrs = histories.stream()
                .map(this::resolvePnr)
                .filter(pnr -> pnr != null && !pnr.isBlank() && !"—".equals(pnr))
                .collect(Collectors.toSet());

        Map<String, BookingContext> contexts = new HashMap<>();
        for (String pnr : pnrs) {
            List<Booking> bookings = bookingRepository.findByPnrIgnoreCaseOrderByCreatedAtDesc(pnr);
            if (bookings.isEmpty()) {
                continue;
            }

            Booking booking = bookings.get(0);
            String agency = resolveAgencyName(booking.getCreatedBy() != null ? booking.getCreatedBy().getId() : null);
            String flightDate = null;
            TravelInformation travelInfo = travelInformationRepository.findByBookingId(booking.getId());
            if (travelInfo != null && travelInfo.getDepartureDate() != null) {
                flightDate = travelInfo.getDepartureDate();
            }
            contexts.put(pnr.toUpperCase(), new BookingContext(agency, flightDate));
        }
        return contexts;
    }

    private Map<Long, BigDecimal> computeOutstandingBalances(
            List<SupplierTransactionHistory> ascendingHistories,
            Map<Long, InvoiceItem> invoiceItemMap,
            BigDecimal initialBalance) {

        Map<Long, BigDecimal> balances = new HashMap<>();
        BigDecimal running = nullToZero(initialBalance);

        for (SupplierTransactionHistory history : ascendingHistories) {
            Amounts amounts = resolveAmounts(history, invoiceItemMap);
            BigDecimal deposit = amounts.deposit() != null ? amounts.deposit() : BigDecimal.ZERO;
            BigDecimal purchase = amounts.purchase() != null ? amounts.purchase() : BigDecimal.ZERO;
            running = running.add(purchase).subtract(deposit);
            balances.put(history.getId(), running);
        }

        return balances;
    }

    private SupplierTransactionReportSummaryDTO buildSummary(
            List<SupplierTransactionHistory> histories,
            Map<Long, InvoiceItem> invoiceItemMap,
            BigDecimal totalPayable,
            BigDecimal totalPaid,
            BigDecimal initialBalance) {

        BigDecimal totalSell = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalDeposit = BigDecimal.ZERO;
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        for (SupplierTransactionHistory history : histories) {
            Amounts amounts = resolveAmounts(history, invoiceItemMap);
            totalSell = totalSell.add(nullToZero(amounts.sell()));
            totalPurchase = totalPurchase.add(nullToZero(amounts.purchase()));
            totalDeposit = totalDeposit.add(nullToZero(amounts.deposit()));
            totalProfitLoss = totalProfitLoss.add(nullToZero(amounts.profitLoss()));
        }

        BigDecimal payable = nullToZero(totalPayable);
        BigDecimal paid = nullToZero(totalPaid);
        BigDecimal opening = nullToZero(initialBalance);

        return SupplierTransactionReportSummaryDTO.builder()
                .totalSell(totalSell)
                .totalPurchase(totalPurchase)
                .totalDeposit(totalDeposit)
                .totalProfitLoss(totalProfitLoss)
                .outstandingBalance(opening.add(payable).subtract(paid))
                .build();
    }

    private SupplierTransactionReportRowDTO toReportRow(
            SupplierTransactionHistory entity,
            Map<Long, InvoiceItem> invoiceItemMap,
            Map<String, BookingContext> bookingContextMap,
            Map<Long, BigDecimal> outstandingById) {

        Amounts amounts = resolveAmounts(entity, invoiceItemMap);
        String pnr = resolvePnr(entity);
        BookingContext bookingContext = pnr != null
                ? bookingContextMap.get(pnr.toUpperCase())
                : null;

        String agency = resolveAgencyName(entity.getAgencyId());
        if ((agency == null || agency.isBlank()) && entity.getInvoiceItemId() != null) {
            InvoiceItem item = invoiceItemMap.get(entity.getInvoiceItemId());
            if (item != null && item.getAgencyUser() != null) {
                agency = resolveAgencyName(item.getAgencyUser().getId());
            }
        }
        if ((agency == null || agency.isBlank()) && bookingContext != null) {
            agency = bookingContext.agency();
        }

        String flightDate = detailValue(entity, "flightDate");
        if ((flightDate == null || flightDate.isBlank()) && bookingContext != null) {
            flightDate = bookingContext.flightDate();
        }

        return SupplierTransactionReportRowDTO.builder()
                .date(timestampMapper.toRequestUserTime(
                        entity.getCreatedDate(),
                        entity.getCreatedTimeOffset()))
                .pnr(pnr)
                .agency(agency)
                .paxName(detailValue(entity, "paxNames"))
                .route(detailValue(entity, "route"))
                .flightDate(flightDate)
                .ticketNo(detailValue(entity, "ticketNumber"))
                .purchase(amounts.purchase())
                .sell(amounts.sell())
                .profitLoss(amounts.profitLoss())
                .depositAmount(amounts.deposit())
                .outstandingBalance(outstandingById.get(entity.getId()))
                .build();
    }

    private Amounts resolveAmounts(SupplierTransactionHistory entity, Map<Long, InvoiceItem> invoiceItemMap) {
        BigDecimal purchase = BigDecimal.ZERO;
        BigDecimal sell = BigDecimal.ZERO;

        if (entity.getInvoiceItemId() != null) {
            InvoiceItem item = invoiceItemMap.get(entity.getInvoiceItemId());
            if (item != null) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                purchase = item.getBuyPrice().multiply(BigDecimal.valueOf(qty));
                sell = item.getSellPrice().multiply(BigDecimal.valueOf(qty));
            }
        } else if (entity.getPayableAmount() != null) {
            purchase = entity.getPayableAmount();
        }

        BigDecimal deposit = entity.getPaidAmount() != null ? entity.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal profitLoss = sell.subtract(purchase);

        return new Amounts(purchase, sell, profitLoss, deposit);
    }

    private void initializeDetails(List<SupplierTransactionHistory> histories) {
        histories.forEach(history -> {
            if (history.getDetails() != null) {
                history.getDetails().size();
            }
        });
    }

    private String resolvePnr(SupplierTransactionHistory entity) {
        String pnr = detailValue(entity, "pnr");
        if (pnr != null && !pnr.isBlank() && !"—".equals(pnr)) {
            return pnr;
        }

        String title = entity.getTitle();
        if (title == null) {
            return null;
        }

        if (title.startsWith("Import PNR: ")) {
            return title.substring("Import PNR: ".length()).trim();
        }
        if (title.startsWith("Manual booking: ")) {
            return title.substring("Manual booking: ".length()).trim();
        }

        return null;
    }

    private String detailValue(SupplierTransactionHistory entity, String key) {
        if (entity.getDetails() == null) {
            return null;
        }

        return entity.getDetails().stream()
                .filter(d -> key.equalsIgnoreCase(d.getKey()))
                .map(d -> d.getValue())
                .findFirst()
                .orElse(null);
    }

    private String resolveAgencyName(Long agencyUserId) {
        if (agencyUserId == null) {
            return null;
        }

        try {
            BusinessDto businessDto = businessService.getBusinessByUserId(agencyUserId);
            return businessDto != null ? businessDto.getCompanyName() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record Amounts(BigDecimal purchase, BigDecimal sell, BigDecimal profitLoss, BigDecimal deposit) {}

    private record BookingContext(String agency, String flightDate) {}


    @Transactional
    public SupplierTransactionHistoryDTO createSupplierTransactionHistory(
            String provider,
            Long authUserId,
            SupplierTransactionCreateDTO request
    ) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        LocalDateTime now = UserDateTimeUtil.now();

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier"));

        Ledger ledger = ledgerRepository.findById(request.getLedgerId())
                .orElseThrow(() -> new ResourceNotFoundException("Ledger"));

        User agencyUser = isAdmin ? null : resolveAgencyUser(authUserId);

        bankLedgerService.validateSupplierBankPayment(request.getPaymentType(), request.getDepositBankId());
        DepositBank depositBank = null;
        if (request.getPaymentType() == ManualInvoicePaymentType.BANK) {
            depositBank = bankLedgerService.resolveActiveBank(request.getDepositBankId());
        }

        SupplierTransactionHistory history = buildHistory(request, ledger, agencyUser, now, depositBank);
        history = supplierTransactionHistoryRepository.save(history);

        updateSupplierAmounts(supplier, history.getPaidAmount());
        supplierRepository.save(supplier);

        if (request.getPaymentType() == ManualInvoicePaymentType.BANK && depositBank != null) {
            bankLedgerService.recordSupplierPayment(
                    depositBank.getId(),
                    history.getId(),
                    history.getPaidAmount(),
                    depositBank.getCurrency(),
                    history.getDescription(),
                    authUserId
            );
        }

        return toDto(history);
    }

    private SupplierTransactionHistory buildHistory(
            SupplierTransactionCreateDTO request,
            Ledger ledger,
            User agencyUser,
            LocalDateTime now,
            DepositBank depositBank
    ) {
        SupplierTransactionHistory history = new SupplierTransactionHistory();

        history.setSupplierId(request.getSupplierId());
        history.setLedgerId(ledger.getId());
        history.setPaidAmount(request.getAmount());
        history.setDescription(request.getDescription());
        history.setAttachments(request.getAttachment());
        history.setCreatedDate(now);
        history.setCreatedTimeOffset(UserDateTimeUtil.currentOffset());
        history.setPaymentType(request.getPaymentType());
        history.setDepositBank(depositBank);

        if (agencyUser != null) {
            history.setAgencyId(agencyUser.getId());
        }

        return history;
    }

    private void updateSupplierAmounts(Supplier supplier, BigDecimal historyPaid) {
        BigDecimal currentAmount = supplier.getPaidAmount() != null
                ? supplier.getPaidAmount()
                : BigDecimal.ZERO;
        BigDecimal paid = historyPaid != null ? historyPaid : BigDecimal.ZERO;
        supplier.setPaidAmount(currentAmount.add(paid));
    }

    private User resolveAgencyUser(Long authUserId) {
        if (authUserId == null) {
            throw ServiceExceptions.accessDenied("Unauthorized");
        }

        User user = userRepository.findById(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        return user.getParentUser() != null ? user.getParentUser() : user;
    }

    private SupplierTransactionHistoryDTO toDto(SupplierTransactionHistory entity) {
        SupplierTransactionHistoryDTO dto = new SupplierTransactionHistoryDTO();

        // Supplier
        if (entity.getSupplierId() != null) {
            supplierRepository.findById(entity.getSupplierId()).ifPresent(supplier -> {
                String label = supplier.getTitle() != null && !supplier.getTitle().isBlank()
                        ? supplier.getTitle()
                        : supplier.getName();
                dto.setSupplier(new CreatorDto(supplier.getId(), label));
            });
        }

        // Agency
        if (entity.getAgencyId() != null) {
            BusinessDto businessDto = businessService.getBusinessByUserId(entity.getAgencyId());
            if (businessDto != null) {
                dto.setAgency(new CreatorDto(businessDto.getId(), businessDto.getCompanyName()));
            }
        }

        Ledger ledger = ledgerRepository.findById(entity.getLedgerId()).orElseThrow(() -> new ResourceNotFoundException("Ledger"));

        dto.setId(entity.getId());
        dto.setCreatedDate(timestampMapper.toRequestUserTime(
                entity.getCreatedDate(), entity.getCreatedTimeOffset()));
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setPayableAmount(entity.getPayableAmount());
        dto.setInvoiceId(entity.getInvoiceId());
        dto.setLedger(new LedgerShortDTO(ledger.getId(), ledger.getTitle()));
        dto.setInvoiceItemId(entity.getInvoiceItemId());
        dto.setTitle(entity.getDescription());
        dto.setDescription(entity.getTitle());
        dto.setPaymentType(entity.getPaymentType());
        dto.setDepositBank(bankLedgerService.toSummary(entity.getDepositBank()));

        if (entity.getDetails() != null && !entity.getDetails().isEmpty()) {
            List<SupplierTransactionHistoryDetailDTO> detailDTOs = entity.getDetails().stream()
                    .map(d -> new SupplierTransactionHistoryDetailDTO(d.getId(), d.getKey(), d.getValue()))
                    .collect(java.util.stream.Collectors.toList());
            dto.setDetails(detailDTOs);
        }

        if (entity.getInvoiceItemId() != null) {
            invoiceItemRepository.findById(entity.getInvoiceItemId()).ifPresent(item -> {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                dto.setOriginalPrice(item.getBuyPrice().multiply(BigDecimal.valueOf(qty)));
                dto.setBookingPrice(item.getSellPrice().multiply(BigDecimal.valueOf(qty)));
            });
        }

        return dto;
    }
}
