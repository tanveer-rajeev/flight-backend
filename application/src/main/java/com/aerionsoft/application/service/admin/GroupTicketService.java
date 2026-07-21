package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.entity.client.*;
import com.aerionsoft.application.repository.client.*;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.dto.admin.GroupTicket.FlightInfoDTO;
import com.aerionsoft.application.dto.admin.GroupTicket.GroupTicketDTO;
import com.aerionsoft.application.dto.admin.GroupTicket.GroupTicketLegDTO;
import com.aerionsoft.application.dto.admin.GroupTicket.PassengerFareDTO;
import com.aerionsoft.application.dto.admin.GroupTicket.Records;
import com.aerionsoft.application.dto.common.PaginationResponseDto;
import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.entity.group.FlightInfo;
import com.aerionsoft.application.entity.group.GroupTicket;
import com.aerionsoft.application.entity.group.PassengerFare;
import com.aerionsoft.application.enums.group.GroupTicketType;
import com.aerionsoft.application.enums.finance.AccountHeadType;
import com.aerionsoft.application.enums.client.InvoiceStatus;
import com.aerionsoft.application.enums.client.InvoiceType;
import com.aerionsoft.application.enums.common.UsingPortal;
import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.repository.finance.AccountHeadRepository;
import com.aerionsoft.application.repository.business.BusinessRepository;
import com.aerionsoft.application.repository.group.GroupTicketRepository;
import com.aerionsoft.application.service.common.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupTicketService {
    @Autowired
    private GroupTicketRepository groupTicketRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private InvoiceLedgerRepository ledgerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private AccountHeadRepository accountHeadRepository;

    @Autowired
    private SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;

    @Autowired
    private BusinessRepository businessRepository;

    private static int getNewBookedQuantity(Integer qty, PassengerFare passengerFare) {
        Integer quantity = passengerFare.getQuantity();
        Integer bookedQuantity = passengerFare.getBookedQuantity();

        if (bookedQuantity > quantity) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid state: booked quantity exceeds total quantity");
        }

        int newBookedQuantity = bookedQuantity + qty;

        if (newBookedQuantity < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Cannot reduce booked quantity below zero");
        }

        if (newBookedQuantity > quantity) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Cannot adjust booking, new booked quantity exceeds available quantity");
        }
        return newBookedQuantity;
    }

    @Transactional
    public GroupTicketDTO createGroupTicket(GroupTicketDTO dto) {
        String gfCode = dto.getGfCode();

        Optional<GroupTicket> existingOpt = groupTicketRepository.findById(gfCode);

        if (existingOpt.isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "GroupTicket already exists");
        }

        if (dto.getGdsPnr() != null &&
                groupTicketRepository.findByGdsPnr(dto.getGdsPnr()) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "GroupTicket with GDS PNR already exists");
        }

        normalizeAndValidateFlightInputs(dto);
        GroupTicket entity = toEntity(dto);
        GroupTicket saved = groupTicketRepository.save(entity);

        // Auto-create invoice for this group ticket
        try {
            createInvoiceForGroupTicket(saved);
        } catch (Exception e) {
            log.warn("Could not auto-create invoice for group ticket [{}]: {}", saved.getGfCode(), e.getMessage());
        }

        return toDTO(saved);
    }

    /**
     * Automatically creates an invoice when a group ticket is created.
     * - Ledger: find by title matching GroupTicket title; if not found, create new admin ledger.
     * - Invoice items: one per PassengerFare (quantity = fare.quantity, sellPrice = baseFare + equivalentTaxes, buyPrice = costing).
     * - AccountHead: find by title "GROUP TICKET" with ADMIN portal; if not found, create it.
     * - Supplier: taken from the group ticket's supplier field (if set).
     */
    private void createInvoiceForGroupTicket(GroupTicket ticket) {
        if (ticket.getSupplier() == null) {
            log.info("No supplier set on group ticket [{}]; skipping invoice creation.", ticket.getGfCode());
            return;
        }

        List<PassengerFare> fares = ticket.getPassengerFares();
        if (fares == null || fares.isEmpty()) {
            log.info("No passenger fares on group ticket [{}]; skipping invoice creation.", ticket.getGfCode());
            return;
        }

        LocalDateTime now = UserDateTimeUtil.now();
        Supplier supplier = ticket.getSupplier();

        // 1. Resolve or create ledger by title (admin ledger = agencyId IS NULL)
        String ledgerTitle = "Group Ticket" ;
        Ledger ledger = ledgerRepository.findByTitleAndAgencyIdIsNull(ledgerTitle)
                .orElseGet(() -> {
                    Ledger newLedger = Ledger.builder()
                            .title(ledgerTitle)
                            .description("Auto-created for group ticket " + ticket.getTitle())
                            .agencyId(null)
                            .createdBy(1L)   // system/admin user
                            .createdAt(now)
                            .build();
                    return ledgerRepository.save(newLedger);
                });

        // 2. Resolve or create AccountHead "GROUP TICKET" for ADMIN portal
        AccountHead accountHead = accountHeadRepository
                .findByAccountHeadTitleAndUsingPortal("GROUP TICKET", UsingPortal.ADMIN)
                .orElseGet(() -> {
                    AccountHead ah = AccountHead.builder()
                            .accountHeadTitle("GROUP TICKET")
                            .type(AccountHeadType.EXPENSE)
                            .parentId(0L)
                            .usingPortal(UsingPortal.ADMIN)
                            .createdBy(1L)
                            .updatedBy(1L)
                            .build();
                    return accountHeadRepository.save(ah);
                });

        Integer totalQuantity = fares.stream()
                .map(fare -> fare.getQuantity() != null ? fare.getQuantity() : 1)
                .reduce(0, Integer::sum);


        BigDecimal totalAmount = BigDecimal.valueOf(ticket.getCosting());

        // 4. Build and save the invoice
        Invoice invoice = Invoice.builder()
                .ledger(ledger)
                .traveller(null)
                .invoiceTitle("Group Ticket: " + ticket.getTitle())
                .invoiceDetails("Auto-generated invoice for group ticket " + ticket.getTitle() + " with " + totalQuantity + " passengers")
                .invoiceDate(LocalDate.now())
                .paymentMethod("CASH")
                .invoiceAmount(totalAmount)
                .invoiceDiscount(BigDecimal.ZERO)
                .invoiceServiceCharge(BigDecimal.ZERO)
                .invoiceRevenue(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING)
                .createdBy(1L)
                .createdAt(now)
                .agencyUser(null)
                .build();
        invoice = invoiceRepository.save(invoice);

        // 5. Build invoice items — one per passenger fare
        List<InvoiceItem> itemsToSave = new ArrayList<>();

        itemsToSave.add(InvoiceItem.builder()
                .invoice(invoice)
                .supplier(supplier)
                .accountHead(accountHead)
                .title("PAX - " + ticket.getGfCode())
                .invoiceType(InvoiceType.FLIGHT)
                .quantity(1)
                .sellPrice(BigDecimal.valueOf(0.0))  // Since we are not calculating sell price per fare, set to 0.0 or you can calculate as needed
                .buyPrice(ticket.getCosting() != null ? BigDecimal.valueOf(ticket.getCosting()) : BigDecimal.ZERO)
                .step(1)
                .createdBy(1L)
                .createAt(now)
                .build());

        List<InvoiceItem> savedItems = invoiceItemRepository.saveAll(itemsToSave);


        // 6. Update supplier payable amounts & create transaction histories
        BigDecimal totalRevenue = BigDecimal.ZERO;
        List<SupplierTransactionHistory> transactionHistories = new ArrayList<>();

        // Since we are creating only one invoice item for the entire group ticket, we will create a single transaction history entry for the total amount. If you want to create individual entries per fare, you can uncomment the loop below and adjust the logic accordingly.

        supplier.setPayableAmount(
                (supplier.getPayableAmount() != null ? supplier.getPayableAmount() : BigDecimal.ZERO)
                        .add(BigDecimal.valueOf(ticket.getCosting()))
        );
        transactionHistories.add(SupplierTransactionHistory.builder()
                .invoiceItemId(savedItems.get(0).getId())
                .invoiceId(invoice.getId())
                .agencyId(null)
                .ledgerId(ledger.getId())
                .supplierId(supplier.getId())
                .payableAmount(BigDecimal.valueOf(ticket.getCosting()))
                .title("Group Ticket: " + ticket.getTitle())
                .description("Auto-created invoice for group ticket " + ticket.getTitle() + " with " + totalQuantity + " passengers")
                .createdDate(now)
                .build());


//        for (int i = 0; i < fares.size(); i++) {
//            PassengerFare fare = fares.get(i);
//            InvoiceItem savedItem = savedItems.get(i);
//
//            int qty = fare.getQuantity() != null ? fare.getQuantity() : 1;
//            double costing = ticket.getCosting() != null ? ticket.getCosting() : 0.0;
//            double baseFare = fare.getBaseFare() != null ? fare.getBaseFare() : 0.0;
//            double taxes = fare.getEquivalentTaxes() != null ? fare.getEquivalentTaxes() : 0.0;
//
//            BigDecimal itemBuyTotal = BigDecimal.valueOf(costing * qty);
//            BigDecimal itemSellTotal = BigDecimal.valueOf((baseFare + taxes) * qty);
//            totalRevenue = totalRevenue.add(itemSellTotal.subtract(itemBuyTotal));
//
//            supplier.setPayableAmount(
//                    (supplier.getPayableAmount() != null ? supplier.getPayableAmount() : BigDecimal.ZERO)
//                            .add(itemBuyTotal)
//            );
//
//            transactionHistories.add(SupplierTransactionHistory.builder()
//                    .invoiceItemId(savedItem.getId())
//                    .invoiceId(invoice.getId())
//                    .agencyId(null)
//                    .ledgerId(ledger.getId())
//                    .supplierId(supplier.getId())
//                    .payableAmount(itemBuyTotal)
//                    .title("Group Ticket: " + ticket.getGfCode())
//                    .description(fare.getFareBasis())
//                    .createdDate(now)
//                    .build());
//        }

        supplierRepository.save(supplier);
        supplierTransactionHistoryRepository.saveAll(transactionHistories);

        // 7. Update invoice revenue
        invoice.setInvoiceRevenue(totalRevenue);
        invoiceRepository.save(invoice);

        log.info("Auto-created invoice [{}] for group ticket [{}]", invoice.getId(), ticket.getGfCode());
    }

    @Transactional(readOnly = true)
    public List<GroupTicketDTO> getGroupSpecialFares(GroupTicketType ticketType) {
        LocalDate today = LocalDate.now();

        List<GroupTicket> tickets = groupTicketRepository.findByDepartureDateAfterOrderByDepartureDateAsc(today);

        tickets = tickets.stream()
                .filter(ticket -> ticketType.getValue().equalsIgnoreCase(ticket.getTicketType()))
                .toList();

        List<GroupTicketDTO> groupTicketDTOList = tickets.stream()
                .map(this::toDTO)
                .toList();


        return groupTicketDTOList.stream().filter(dto -> "ACTIVE".equalsIgnoreCase(dto.getStatus()))
                .filter(dto -> dto.getSaleStatus() == null || !"OFFLINE".equalsIgnoreCase(dto.getSaleStatus()))
                .map(dto -> {
                    List<PassengerFareDTO> fareDTOList = dto.getPassengerFares();

                    if (fareDTOList != null) {
                        fareDTOList.forEach(fareDTO -> {
                            String currency = fareDTO.getCurrency();
                            Double baseFare = fareDTO.getBaseFare();
                            Double taxes = fareDTO.getEquivalentTaxes();

                            if (currency != null && baseFare != null && taxes != null) {
                                Double convertedBaseFare =
                                        currencyService.convertCurrency(
                                                String.valueOf(baseFare),
                                                currency,
                                                "USD",
                                                "GROUP"
                                        );

                                Double convertedTaxes =
                                        currencyService.convertCurrency(
                                                String.valueOf(taxes),
                                                currency,
                                                "USD",
                                                "GROUP"
                                        );

                                fareDTO.setPublishedFare(
                                        String.valueOf(convertedBaseFare + convertedTaxes)
                                );
                                fareDTO.setCurrency("USD");
                            }
                        });
                    }

                    return dto;
                })
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public PaginationResponseDto<GroupTicketDTO> listGroupTickets(
            String airlineCode,
            String status,
            String pnr,
            String ticketType,
            Long agencyId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );


        Specification<GroupTicket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (airlineCode != null && !airlineCode.isBlank()) {
                predicates.add(cb.equal(root.get("airlineCode"), airlineCode));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (pnr != null && !pnr.isBlank()) {
                predicates.add(cb.equal(root.get("airlinePnr"), pnr));
            }

            if (ticketType != null && !ticketType.isBlank()) {
                GroupTicketType parsedTicketType = GroupTicketType.fromValue(ticketType);
                predicates.add(cb.equal(root.get("ticketType"), parsedTicketType.getValue()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GroupTicket> ticketPage = groupTicketRepository.findAll(spec, pageable);

        // Resolve agency currency if agencyId is provided
        String agencyCurrency = null;
        if (agencyId != null) {
            BusinessEntity business = businessRepository.findById(agencyId).orElse(null);
            if (business != null && business.getMotherUser() != null
                    && business.getMotherUser().getCurrency() != null) {
                agencyCurrency = business.getMotherUser().getCurrency();
            }
        }

        final String targetCurrency = agencyCurrency;

        List<GroupTicketDTO> dtoList = ticketPage.getContent().stream()
                .map(this::toDTO)
                .map(dto -> {
                    if (targetCurrency != null && dto.getPassengerFares() != null) {
                        dto.getPassengerFares().forEach(fareDTO -> convertFareCurrency(fareDTO, targetCurrency));
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return PaginationResponseDto.<GroupTicketDTO>builder()
                .content(dtoList)
                .page(ticketPage.getNumber())
                .size(ticketPage.getSize())
                .totalElements(ticketPage.getTotalElements())
                .totalPages(ticketPage.getTotalPages())
                .first(ticketPage.isFirst())
                .last(ticketPage.isLast())
                .build();
    }

    /**
     * Converts all monetary fields of a PassengerFareDTO from the fare's own currency
     * to the given target currency using GROUP provider rates.
     */
    private void convertFareCurrency(PassengerFareDTO fareDTO, String targetCurrency) {
        String fromCurrency = fareDTO.getCurrency();
        if (fromCurrency == null || fromCurrency.equalsIgnoreCase(targetCurrency)) {
            return;
        }

        if (fareDTO.getBaseFare() != null) {
            fareDTO.setBaseFare(currencyService.convertCurrency(
                    String.valueOf(fareDTO.getBaseFare()), fromCurrency, targetCurrency, "GROUP"));
        }

        if (fareDTO.getEquivalentBaseFare() != null) {
            fareDTO.setEquivalentBaseFare(currencyService.convertCurrency(
                    String.valueOf(fareDTO.getEquivalentBaseFare()), fromCurrency, targetCurrency, "GROUP"));
        }

        if (fareDTO.getEquivalentTaxes() != null) {
            fareDTO.setEquivalentTaxes(currencyService.convertCurrency(
                    String.valueOf(fareDTO.getEquivalentTaxes()), fromCurrency, targetCurrency, "GROUP"));
        }

        if (fareDTO.getPublishedFare() != null) {
            try {
                Double converted = currencyService.convertCurrency(
                        fareDTO.getPublishedFare(), fromCurrency, targetCurrency, "GROUP");
                fareDTO.setPublishedFare(String.valueOf(converted));
            } catch (NumberFormatException ignored) {
                // publishedFare may not be a clean numeric string; leave as-is
            }
        }

        // Record the exchange rate used for this conversion
        try {
            double rate = Double.parseDouble(
                    currencyService.getExchangeRate(fromCurrency, targetCurrency, "GROUP"));
            fareDTO.setExchangeRate(rate);
        } catch (Exception ignored) {
            // leave existing exchangeRate if lookup fails
        }

        fareDTO.setCurrency(targetCurrency);
    }

    @Transactional(readOnly = true)
    public List<GroupTicketDTO> getAvailableTickets(String departureDate, String origin, String destination,String returnDate) {
        LocalDate departureDateLocal;

        // Only parse if not null or empty
        if (departureDate != null && !departureDate.isBlank()) {
            departureDateLocal = LocalDate.parse(departureDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            departureDateLocal = null;
        }


        // Query DB only if date filter provided, else fetch all
        List<GroupTicket> tickets = groupTicketRepository.findByDepartureDateAndOriginAndDestination(departureDateLocal, origin, destination);


        return tickets.stream()
                .filter(ticket -> {
//                    return !ticket.getStatus().equalsIgnoreCase("Expired");
                    if(ticket.getStatus().equalsIgnoreCase("Expired")){
                        return false;
                    }

                    if(ticket.getSaleStatus().equalsIgnoreCase("OFFLINE")){
                        return false;
                    }

                    List<PassengerFare> fares = ticket.getPassengerFares();
                    if (fares == null || fares.isEmpty()) {
                        return false;
                    }

                    PassengerFare fare = fares.get(0);
                    if (fare == null || fare.getBookedQuantity() >= fare.getQuantity()) {
                        return false;
                    }

                    String flightType = ticket.getFlightType();
                    boolean isRoundTrip = "Round-Trip".equalsIgnoreCase(flightType);

                    if (isRoundTrip && (returnDate == null || returnDate.isBlank())) {
                        return false;
                    }

                    if (departureDateLocal != null) {
                        LocalDate depDate = ticket.getDepartureDate();
                        return depDate != null && depDate.equals(departureDateLocal);
                    }
                    return true;
                })
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupTicketDTO getGroupTicket(String gfCode) {
        GroupTicket ticket = groupTicketRepository.findById(gfCode)
                .orElseThrow(() -> new EntityNotFoundException("Group ticket not found"));

        return toDTO(ticket);
    }

    /**
     * Returns the GroupTicket entity directly (not DTO).
     * Used by BookingCoordinatorService for group-ticket-manual booking.
     */
    @Transactional(readOnly = true)
    public GroupTicket getGroupTicketEntity(String gfCode) {
        return groupTicketRepository.findById(gfCode)
                .orElseThrow(() -> new EntityNotFoundException("Group ticket not found with GF code: " + gfCode));
    }

    @Transactional(readOnly = true)
    public GroupTicket findGroupTicketByGdsPnr(String gdsPnr) {
        return groupTicketRepository.findByGdsPnr(gdsPnr);
    }

    public void deleteGroupTicket(String gfCode) {
        GroupTicket ticket = groupTicketRepository.findById(gfCode)
                .orElseThrow(() -> new EntityNotFoundException("Group ticket not found"));
        groupTicketRepository.delete(ticket);
    }

    private GroupTicket toEntity(GroupTicketDTO dto) {
        GroupTicket entity = new GroupTicket();
        entityFromDTO(entity, dto);
        return entity;
    }

    public GroupTicketDTO updateGroupTicket(String gfCode, GroupTicketDTO dto) {
        GroupTicket existing = groupTicketRepository.findById(gfCode)
                .orElseThrow(() -> new EntityNotFoundException("Group ticket not found"));

        normalizeAndValidateFlightInputs(dto);
        updateEntityFromDTO(existing, dto);
        GroupTicket updated = groupTicketRepository.save(existing);
        return toDTO(updated);
    }

    private void updateEntityFromDTO(GroupTicket entity, GroupTicketDTO dto) {
        entity.setTitle(dto.getTitle());
        mapDtoToEntity(entity, dto);

        if (dto.getFlightInfos() != null) {
            entity.getFlightInfo().clear();
            entity.getFlightInfo().addAll(
                    dto.getFlightInfos().stream()
                            .map(this::toFlightInfoEntity)
                            .toList()
            );
        }

        if (dto.getPassengerFares() != null) {
            updatePassengerFaresFromDTO(entity, dto.getPassengerFares());
        }
    }

    private void mapDtoToEntity(GroupTicket entity, GroupTicketDTO dto) {
        entity.setType(dto.getType());
        entity.setTicketType(dto.getTicketType().getValue());
        entity.setStatus(dto.getStatus());
        entity.setDescription(dto.getDescription());
        entity.setSpecialInstructions(dto.getSpecialInstructions());
        entity.setAirlineCode(dto.getAirlineCode());
        entity.setAirlineName(dto.getAirlineName());
        entity.setVendorName(dto.getVendorName());
        entity.setBookingStarts(dto.getBookingStarts());
        entity.setBookingEnds(dto.getBookingEnds());
        entity.setOrigin(dto.getOrigin());
        entity.setDestination(dto.getDestination());
        entity.setFareCurrency(dto.getFareCurrency());
        entity.setGdsPnr(dto.getGdsPnr());
        entity.setAirlinePnr(dto.getAirlinePnr());
        entity.setArrivalTime(dto.getArrivalTime());
        entity.setDepartureTime(dto.getDepartureTime());
        entity.setDepartureDate(dto.getDepartureDate());
        entity.setArrivalDate(dto.getArrivalDate());
        entity.setFlightType(dto.getFlightType());
        entity.setCosting(dto.getCosting());
        entity.setSaleStatus(dto.getSaleStatus());

        if (dto.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + dto.getSupplierId()));
            entity.setSupplier(supplier);
        } else {
            entity.setSupplier(null);
        }
    }

    private void updatePassengerFaresFromDTO(GroupTicket entity, List<PassengerFareDTO> dtoFares) {
        if (entity.getPassengerFares() == null) {
            entity.setPassengerFares(new ArrayList<>());
        }
        List<PassengerFare> existingFares = entity.getPassengerFares();

        for (int i = 0; i < dtoFares.size(); i++) {
            PassengerFareDTO fareDto = dtoFares.get(i);
            if (i < existingFares.size()) {
                updatePassengerFareFromDTO(existingFares.get(i), fareDto);
            } else {
                existingFares.add(toPassengerFareEntity(fareDto));
            }
        }

        while (existingFares.size() > dtoFares.size()) {
            existingFares.remove(existingFares.size() - 1);
        }
    }

    private void updatePassengerFareFromDTO(PassengerFare entity, PassengerFareDTO dto) {
        entity.setFareBasis(dto.getFareBasis());
        entity.setQuantity(dto.getQuantity());
        entity.setCurrency(dto.getCurrency());
        entity.setBaseFare(dto.getBaseFare());
        entity.setEquivalentBaseFare(dto.getEquivalentBaseFare());
        entity.setEquivalentTaxes(dto.getEquivalentTaxes());
        entity.setExchangeRate(dto.getExchangeRate());
        entity.setBaggageKg(dto.getBaggageKg());
    }

    private void entityFromDTO(GroupTicket entity, GroupTicketDTO dto) {
        entity.setGfCode(dto.getGfCode());
        mapDtoToEntity(entity, dto);

        // Initialize if null
        if (dto.getFlightInfos() != null) {
            if (entity.getFlightInfo() == null) {
                entity.setFlightInfo(new ArrayList<>());
            }
            entity.getFlightInfo().addAll(
                    dto.getFlightInfos().stream()
                            .map(this::toFlightInfoEntity)
                            .toList()
            );
        }

        if (dto.getPassengerFares() != null) {
            if (entity.getPassengerFares() == null) {
                entity.setPassengerFares(new ArrayList<>());
            }
            entity.getPassengerFares().addAll(
                    dto.getPassengerFares().stream()
                            .map(this::toPassengerFareEntity)
                            .toList()
            );
        }
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
            dto.setSupplierTitle(entity.getSupplier().getTitle());
        }

        if (entity.getFlightInfo() != null) {
            List<FlightInfoDTO> flightInfos = entity.getFlightInfo().stream()
                    .map(this::toFlightInfoDTO)
                    .collect(Collectors.toList());
            dto.setFlightInfos(flightInfos);
            dto.setLegs(buildLegsFromFlightInfos(flightInfos));
        }

        if (entity.getPassengerFares() != null) {
            dto.setPassengerFares(entity.getPassengerFares().stream()
                    .map(this::toPassengerFareDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private FlightInfo toFlightInfoEntity(FlightInfoDTO dto) {
        FlightInfo entity = new FlightInfo();
        entity.setOrigin(dto.getOrigin());
        entity.setDestination(dto.getDestination());
        entity.setDepartureDate(dto.getDepartureDate());
        entity.setDepartureTime(dto.getDepartureTime());
        entity.setArrivalDate(dto.getArrivalDate());
        entity.setArrivalTime(dto.getArrivalTime());
        entity.setFlightNumber(dto.getFlightNumber());
        entity.setDurationInMinutes(dto.getDurationInMinutes());
        entity.setStops(dto.getStops());
        entity.setEquipment(dto.getEquipment());
        entity.setOriginTerminal(dto.getOriginTerminal());
        entity.setDestinationTerminal(dto.getDestinationTerminal());
        entity.setLeg(dto.getLeg());
        entity.setSegmentType(normalizeSegmentType(dto.getSegmentType()));
        return entity;
    }

    private FlightInfoDTO toFlightInfoDTO(FlightInfo entity) {
        FlightInfoDTO dto = new FlightInfoDTO();
        dto.setOrigin(entity.getOrigin());
        dto.setDestination(entity.getDestination());
        dto.setArrivalDate(entity.getArrivalDate());
        dto.setArrivalTime(entity.getArrivalTime());
        dto.setDepartureDate(entity.getDepartureDate());
        dto.setDepartureTime(entity.getDepartureTime());
        dto.setFlightNumber(entity.getFlightNumber());
        dto.setDurationInMinutes(entity.getDurationInMinutes());
        dto.setStops(entity.getStops());
        dto.setEquipment(entity.getEquipment());
        dto.setOriginTerminal(entity.getOriginTerminal());
        dto.setDestinationTerminal(entity.getDestinationTerminal());
        dto.setLeg(entity.getLeg() != null && entity.getLeg() >= 1 ? entity.getLeg() : 1);
        dto.setSegmentType(normalizeSegmentType(entity.getSegmentType()));
        return dto;
    }

    private PassengerFare toPassengerFareEntity(PassengerFareDTO dto) {
        PassengerFare entity = new PassengerFare();
        entity.setFareBasis(dto.getFareBasis());
        entity.setQuantity(dto.getQuantity());
        entity.setCurrency(dto.getCurrency());
        entity.setBaseFare(dto.getBaseFare());
        entity.setEquivalentBaseFare(dto.getEquivalentBaseFare());
        entity.setEquivalentTaxes(dto.getEquivalentTaxes());
        entity.setExchangeRate(dto.getExchangeRate());
        entity.setBaggageKg(dto.getBaggageKg());
        return entity;
    }

    private PassengerFareDTO toPassengerFareDTO(PassengerFare entity) {
        PassengerFareDTO dto = new PassengerFareDTO();
        dto.setFareBasis(entity.getFareBasis());
        dto.setQuantity(entity.getQuantity());
        dto.setBookedQuantity(entity.getBookedQuantity());
        dto.setCurrency(entity.getCurrency());
        dto.setBaseFare(entity.getBaseFare());
        dto.setEquivalentBaseFare(entity.getEquivalentBaseFare());
        dto.setEquivalentTaxes(entity.getEquivalentTaxes());

        dto.setExchangeRate(entity.getExchangeRate());
        dto.setBaggageKg(entity.getBaggageKg());
        return dto;
    }

    public void adjustBooking(String gdCode, Integer qty, Records records) {
        GroupTicket ticket = groupTicketRepository.findById(gdCode)
                .orElseThrow(() -> new EntityNotFoundException("Group ticket not found with code: " + gdCode));

        List<PassengerFare> passengerFares = ticket.getPassengerFares();

        if (passengerFares == null || passengerFares.isEmpty()) {
            throw ServiceExceptions.notFound("No passenger fares found for this group ticket");
        }

        // Update all passenger fares with the new booked quantity
        for (PassengerFare passengerFare : passengerFares) {

            String fareBasis = passengerFare.getFareBasis().toUpperCase();
            int count = 0;

            switch (fareBasis) {
                case "ADULT":
                    count = records.getAdult();
                    break;
                case "CHILD":
                    count = records.getChild();
                    break;
                case "INFANT":
                    count = records.getInfant();
                    break;
                default:
                    continue;
            }

            if (count > 0) {
                passengerFare.setBookedQuantity(
                        getNewBookedQuantity(count, passengerFare)
                );
            }
        }

        groupTicketRepository.save(ticket);
    }


    public Double getPriceByGfCode(String gfCode) {
        GroupTicket ticket = groupTicketRepository.findById(gfCode)
                .orElseThrow(() -> ServiceExceptions.notFound("Group ticket not found with code: " + gfCode));

        List<PassengerFare> fares = ticket.getPassengerFares();
        if (fares == null || fares.isEmpty()) {
            throw ServiceExceptions.notFound("No passenger fares found for this group ticket");
        }

        return fares.stream()
                .mapToDouble(PassengerFare::getBaseFare)
                .sum();
    }

    public Double getPriceByGfCode(String gfCode, int adt, int chd, int inf) {
        GroupTicket ticket = groupTicketRepository.findById(gfCode)
                .orElseThrow(() -> ServiceExceptions.notFound("Group ticket not found with code: " + gfCode));

        List<PassengerFare> fares = ticket.getPassengerFares();
        if (fares == null || fares.isEmpty()) {
            throw ServiceExceptions.notFound("No passenger fares found for this group ticket");
        }

        double total = 0;
        for (PassengerFare fare : fares) {
            int paxCount = resolvePaxCountForFareBasis(fare.getFareBasis(), adt, chd, inf);
            if (paxCount <= 0) {
                continue;
            }
            double baseFare = fare.getBaseFare() != null ? fare.getBaseFare() : 0;
            double taxes = fare.getEquivalentTaxes() != null ? fare.getEquivalentTaxes() : 0;
            total += (baseFare + taxes) * paxCount;
        }

        return currencyService.convertCurrency(String.valueOf(total), ticket.getFareCurrency(), "USD", "GROUP");
    }

    /**
     * Returns the buy/cost price for a group booking: costing per pax × total passengers, in USD.
     * Returns null when costing is not set on the ticket.
     */
    public Double getCostingPriceByGfCode(String gfCode, int totalPax) {
        GroupTicket ticket = groupTicketRepository.findById(gfCode)
                .orElseThrow(() -> ServiceExceptions.notFound("Group ticket with code: " + gfCode));

        if (ticket.getCosting() == null || totalPax <= 0) {
            return null;
        }

        double total = ticket.getCosting() * totalPax;
        return currencyService.convertCurrency(String.valueOf(total), ticket.getFareCurrency(), "USD", "GROUP");
    }

    private int resolvePaxCountForFareBasis(String fareBasis, int adt, int chd, int inf) {
        if (fareBasis == null) {
            return 0;
        }
        return switch (fareBasis.toLowerCase()) {
            case "adult" -> adt;
            case "child" -> chd;
            case "infant" -> inf;
            default -> 0;
        };
    }

    /**
     * Auto-expire group tickets whose booking end date is before today.
     * Called by the scheduled job. Skips tickets already marked EXPIRED.
     *
     * @return number of tickets expired in this run
     */
    public int expireGroupTickets() {
        LocalDate today = LocalDate.now();
        List<GroupTicket> expired = groupTicketRepository
                .findByBookingEndsBeforeAndStatusNot(today, "EXPIRED");

        if (expired.isEmpty()) {
            log.info("No group tickets to expire.");
            return 0;
        }

        for (GroupTicket ticket : expired) {
            log.info("Expiring group ticket [{}] – bookingEnds={} status was={}",
                    ticket.getGfCode(), ticket.getBookingEnds(), ticket.getStatus());
            ticket.setStatus("EXPIRED");
        }

        groupTicketRepository.saveAll(expired);
        log.info("Auto-expired {} group ticket(s).", expired.size());
        return expired.size();
    }

    /**
     * Accepts either {@code legs[]} (grouped UI input) or flat {@code flightInfos[]}.
     * When legs are provided they replace flightInfos before persistence.
     */
    private void normalizeAndValidateFlightInputs(GroupTicketDTO dto) {
        if (dto.getLegs() != null && !dto.getLegs().isEmpty()) {
            dto.setFlightInfos(flattenLegsToFlightInfos(dto.getLegs()));
        }

        applyFlightInfoDefaults(dto.getFlightInfos());
        validateFlightInfos(dto);
    }

    private List<FlightInfoDTO> flattenLegsToFlightInfos(List<GroupTicketLegDTO> legs) {
        List<FlightInfoDTO> flattened = new ArrayList<>();
        int autoLeg = 1;

        for (GroupTicketLegDTO leg : legs) {
            if (leg.getSegments() == null || leg.getSegments().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Each leg must contain at least one segment");
            }

            int legNumber = leg.getLeg() != null ? leg.getLeg() : autoLeg;
            String segmentType = normalizeSegmentType(
                    leg.getSegmentType() != null ? leg.getSegmentType() : "ONEWAY");

            for (FlightInfoDTO segment : leg.getSegments()) {
                FlightInfoDTO copy = copyFlightInfo(segment);
                copy.setLeg(legNumber);
                copy.setSegmentType(segmentType);
                flattened.add(copy);
            }
            autoLeg = legNumber + 1;
        }

        return flattened;
    }

    private FlightInfoDTO copyFlightInfo(FlightInfoDTO source) {
        FlightInfoDTO copy = new FlightInfoDTO();
        copy.setOrigin(source.getOrigin());
        copy.setDestination(source.getDestination());
        copy.setDepartureDate(source.getDepartureDate());
        copy.setDepartureTime(source.getDepartureTime());
        copy.setArrivalDate(source.getArrivalDate());
        copy.setArrivalTime(source.getArrivalTime());
        copy.setFlightNumber(source.getFlightNumber());
        copy.setDurationInMinutes(source.getDurationInMinutes());
        copy.setStops(source.getStops());
        copy.setEquipment(source.getEquipment());
        copy.setOriginTerminal(source.getOriginTerminal());
        copy.setDestinationTerminal(source.getDestinationTerminal());
        copy.setLeg(source.getLeg());
        copy.setSegmentType(source.getSegmentType());
        return copy;
    }

    private void applyFlightInfoDefaults(List<FlightInfoDTO> flightInfos) {
        if (flightInfos == null) {
            return;
        }

        int autoLeg = 1;
        Integer currentLeg = null;

        for (FlightInfoDTO flightInfo : flightInfos) {
            if (flightInfo.getLeg() == null) {
                if (currentLeg == null) {
                    flightInfo.setLeg(autoLeg);
                } else {
                    flightInfo.setLeg(currentLeg);
                }
            } else {
                currentLeg = flightInfo.getLeg();
                autoLeg = currentLeg + 1;
            }

            flightInfo.setSegmentType(normalizeSegmentType(flightInfo.getSegmentType()));
        }
    }

    private void validateFlightInfos(GroupTicketDTO dto) {
        List<FlightInfoDTO> flightInfos = dto.getFlightInfos();
        if (flightInfos == null || flightInfos.isEmpty()) {
            return;
        }

        Map<Integer, List<FlightInfoDTO>> byLeg = new LinkedHashMap<>();
        for (FlightInfoDTO flightInfo : flightInfos) {
            if (flightInfo.getOrigin() == null || flightInfo.getOrigin().isBlank()
                    || flightInfo.getDestination() == null || flightInfo.getDestination().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Each flight segment must have origin and destination");
            }

            int leg = flightInfo.getLeg() != null ? flightInfo.getLeg() : 1;
            byLeg.computeIfAbsent(leg, ignored -> new ArrayList<>()).add(flightInfo);
        }

        for (Map.Entry<Integer, List<FlightInfoDTO>> entry : byLeg.entrySet()) {
            List<FlightInfoDTO> segments = entry.getValue();
            for (int i = 0; i < segments.size() - 1; i++) {
                String currentDestination = segments.get(i).getDestination().trim().toUpperCase();
                String nextOrigin = segments.get(i + 1).getOrigin().trim().toUpperCase();
                if (!currentDestination.equals(nextOrigin)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Connecting segments on leg " + entry.getKey()
                                    + " must chain: segment destination must match next segment origin");
                }
            }
        }

        String flightType = dto.getFlightType() != null ? dto.getFlightType().trim() : "";
        if (isRoundTripFlightType(flightType) && !isLegacyOneWayOnlyFlightInfos(flightInfos)) {
            boolean hasReturn = flightInfos.stream()
                    .anyMatch(fi -> "RETURN".equalsIgnoreCase(fi.getSegmentType()));
            if (!hasReturn) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Round-trip group tickets must include at least one RETURN segment (leg 2+)");
            }
        }
    }

    /**
     * Pre-leg-migration data: all segments on leg 1 with ONEWAY type.
     * Skip strict round-trip RETURN validation so old tickets can still be updated.
     */
    private boolean isLegacyOneWayOnlyFlightInfos(List<FlightInfoDTO> flightInfos) {
        return flightInfos.stream().allMatch(fi ->
                (fi.getLeg() == null || fi.getLeg() <= 1)
                        && (fi.getSegmentType() == null
                        || fi.getSegmentType().isBlank()
                        || "ONEWAY".equalsIgnoreCase(fi.getSegmentType())));
    }

    private List<GroupTicketLegDTO> buildLegsFromFlightInfos(List<FlightInfoDTO> flightInfos) {
        if (flightInfos == null || flightInfos.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<FlightInfoDTO>> grouped = new LinkedHashMap<>();
        for (FlightInfoDTO flightInfo : flightInfos) {
            int leg = flightInfo.getLeg() != null ? flightInfo.getLeg() : 1;
            grouped.computeIfAbsent(leg, ignored -> new ArrayList<>()).add(flightInfo);
        }

        List<GroupTicketLegDTO> legs = new ArrayList<>();
        for (Map.Entry<Integer, List<FlightInfoDTO>> entry : grouped.entrySet()) {
            List<FlightInfoDTO> segments = entry.getValue();
            GroupTicketLegDTO legDto = new GroupTicketLegDTO();
            legDto.setLeg(entry.getKey());
            legDto.setSegmentType(normalizeSegmentType(segments.get(0).getSegmentType()));
            legDto.setOrigin(segments.get(0).getOrigin());
            legDto.setDestination(segments.get(segments.size() - 1).getDestination());
            legDto.setSegments(segments);
            legs.add(legDto);
        }

        return legs;
    }

    private String normalizeSegmentType(String segmentType) {
        if (segmentType == null || segmentType.isBlank()) {
            return "ONEWAY";
        }
        String normalized = segmentType.trim().toUpperCase();
        if ("RETURN".equals(normalized)) {
            return "RETURN";
        }
        return "ONEWAY";
    }

    private boolean isRoundTripFlightType(String flightType) {
        if (flightType == null || flightType.isBlank()) {
            return false;
        }
        String normalized = flightType.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return "ROUND_TRIP".equals(normalized) || "ROUNDTRIP".equals(normalized);
    }

}