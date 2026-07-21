package com.aerionsoft.application.service.client;

import com.aerionsoft.application.entity.client.*;
import com.aerionsoft.application.repository.client.*;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.CreatorDto;
import com.aerionsoft.application.dto.SupplierDto;
import com.aerionsoft.application.dto.accounthead.AccountHeadDto;
import com.aerionsoft.application.dto.client.invoice.InvoiceDto;
import com.aerionsoft.application.dto.client.invoice.InvoiceItemDto;
import com.aerionsoft.application.dto.client.invoice.response.InvoiceFullResponseDTO;
import com.aerionsoft.application.dto.client.invoice.response.InvoiceItemResponseDto;
import com.aerionsoft.application.dto.client.invoice.response.InvoiceResponseDto;
import com.aerionsoft.application.entity.AccountHead;
import com.aerionsoft.application.entity.Booking.Traveller;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.enums.client.InvoiceStatus;
import com.aerionsoft.application.repository.finance.AccountHeadRepository;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.repository.booking.TravellerRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final TravellerRepository travellerRepository;
    private final AccountHeadRepository accountHeadRepository;
    private final SupplierRepository supplierRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceDynamicItemRepository invoiceDynamicItemRepository;
    private final InvoiceLedgerRepository ledgerRepository;
    private final AdminUserRepository adminUserRepository;
    private final SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;
    private final TimestampMapper timestampMapper;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            UserRepository userRepository,
            TravellerRepository travellerRepository,
            AccountHeadRepository accountHeadRepository,
            SupplierRepository supplierRepository,
            InvoiceItemRepository invoiceItemRepository,
            InvoiceDynamicItemRepository invoiceDynamicItemRepository,
            InvoiceLedgerRepository ledgerRepository,
            AdminUserRepository adminUserRepository,
            SupplierTransactionHistoryRepository supplierTransactionHistoryRepository,
            TimestampMapper timestampMapper
    ) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.travellerRepository = travellerRepository;
        this.accountHeadRepository = accountHeadRepository;
        this.supplierRepository = supplierRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceDynamicItemRepository = invoiceDynamicItemRepository;
        this.ledgerRepository = ledgerRepository;
        this.adminUserRepository = adminUserRepository;
        this.supplierTransactionHistoryRepository = supplierTransactionHistoryRepository;
        this.timestampMapper = timestampMapper;
    }

    /**
     * Invoice list service
     *
     * @param provider   admin or agency
     * @param authUserId authenticate user id
     * @param page       page number
     * @param size       require item number
     * @return List<InvoiceResponseDto>
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getInvoices(String provider, Long authUserId, int page, int size) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        Page<Invoice> invoices;

        if (isAdmin) {
            invoices = invoiceRepository.findByAgencyUserIsNull(PageRequest.of(page, size));
        } else {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;

            invoices = invoiceRepository.findByAgencyUser(agencyUser, PageRequest.of(page, size));
        }

        return invoices.getContent().stream().map(this::dto).collect(Collectors.toList());
    }

    /**
     * Invoice show service
     *
     * @param provider   admin or agency
     * @param authUserId authenticate user id
     * @param id         invoice id
     * @return InvoiceFullResponseDTO
     */
    @Transactional(readOnly = true)
    public InvoiceFullResponseDTO getInvoice(String provider, Long authUserId, Long id) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        Invoice invoice;

        if (isAdmin) {
            invoice = invoiceRepository.findByIdAndAgencyUserIsNull(id).orElseThrow(() -> new ResourceNotFoundException("Invoice"));
        } else {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;

            invoice = invoiceRepository.findByIdAndAgencyUser(id, agencyUser).orElseThrow(() -> new ResourceNotFoundException("Invoice"));
        }

        return fullDto(invoice);
    }

    /**
     * Invoice create service
     *
     * @param provider   admin or agency
     * @param authUserId authenticate user id
     * @param dto        request data to create invoice
     */
    @Transactional
    public void createInvoice(String provider, Long authUserId, InvoiceDto dto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        Ledger ledger = ledgerRepository.findById(dto.getLedgerId())
                .orElseThrow(() -> new ResourceNotFoundException("Ledger"));

        Traveller traveller = null;

        if (dto.getTravellerId() != null) {
            traveller = travellerRepository.findById(dto.getTravellerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Traveller"));
        }

        User agencyUser = null;

        if (!isAdmin) {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
        }

        // Create invoice (single place)
        Invoice invoice = buildInvoice(dto, ledger, traveller, authUserId, agencyUser);
        invoice = invoiceRepository.save(invoice);

        // Create items + dynamic items (single place)
        createInvoiceItems(invoice, dto.getInvoiceItems(), authUserId, agencyUser);
    }

    /**
     *
     * @param provider   admin or agency
     * @param authUserId authenticate user id
     * @param invoiceId  to update specific invoice id
     * @param dto        request data to update invoice
     */
    @Transactional
    public void updateInvoice(String provider, Long authUserId, Long invoiceId, InvoiceDto dto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        // Load existing invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice"));

        Ledger ledger = ledgerRepository.findById(dto.getLedgerId())
                .orElseThrow(() -> new ResourceNotFoundException("Ledger"));

        Traveller traveller = travellerRepository.findById(dto.getTravellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Traveller"));

        User agencyUser = null;
        if (!isAdmin) {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
        }

        // Update invoice header
        invoice.setLedger(ledger);
        invoice.setTraveller(traveller);
        invoice.setInvoiceTitle(dto.getInvoiceTitle());
        invoice.setInvoiceDetails(dto.getInvoiceDetails());
        invoice.setDocument(dto.getDocument());
        invoice.setInvoiceDate(LocalDate.parse(dto.getInvoiceDate()));
        invoice.setPaymentMethod(dto.getPaymentMethod());
        invoice.setInvoiceAmount(dto.getInvoiceAmount());
        invoice.setInvoiceRevenue(BigDecimal.ZERO);
        invoice.setInvoiceDiscount(dto.getInvoiceDiscount());
        invoice.setInvoiceServiceCharge(dto.getInvoiceServiceCharge());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setUpdatedBy(authUserId);

        if (!isAdmin) {
            invoice.setAgencyUser(agencyUser);
        } else {
            invoice.setAgencyUser(null);
        }

        invoice = invoiceRepository.save(invoice);

        // Remove existing items + dynamic items
        List<InvoiceItem> existingItems = invoiceItemRepository.findByInvoice(invoice);
        if (!existingItems.isEmpty()) {
            invoiceDynamicItemRepository.deleteByInvoiceItemIn(existingItems);
            invoiceItemRepository.deleteAll(existingItems);
        }

        // Recreate items + dynamic items using the same helper as createInvoice
        createInvoiceItems(invoice, dto.getInvoiceItems(), authUserId, agencyUser);
    }

    private Invoice buildInvoice(InvoiceDto dto, Ledger ledger, @Nullable Traveller traveller, Long createdBy, @Nullable User agencyUser) {
        Invoice.InvoiceBuilder builder = Invoice.builder()
                .ledger(ledger)
                .traveller(traveller)
                .invoiceTitle(dto.getInvoiceTitle())
                .invoiceDetails(dto.getInvoiceDetails())
                .document(dto.getDocument())
                .invoiceDate(LocalDate.parse(dto.getInvoiceDate()))
                .paymentMethod(dto.getPaymentMethod())
                .invoiceAmount(dto.getInvoiceAmount())
                .invoiceDiscount(dto.getInvoiceDiscount())
                .invoiceServiceCharge(dto.getInvoiceServiceCharge())
                .status(InvoiceStatus.PENDING)
                .createdBy(createdBy)
                .createdAt(UserDateTimeUtil.now());

        if (agencyUser != null) {
            builder.agencyUser(agencyUser);
        }

        return builder.build();
    }

    private void createInvoiceItems(Invoice invoice, List<InvoiceItemDto> itemDtos, Long authUserId, @Nullable User agencyUser) {

        LocalDateTime now = UserDateTimeUtil.now();

        // Small cache to avoid same supplier/accountHead queries multiple times
        Map<Long, Supplier> supplierCache = new HashMap<>();
        Map<Long, AccountHead> accountHeadCache = new HashMap<>();

        List<InvoiceItem> itemsToSave = new ArrayList<>();

        List<SupplierTransactionHistory> transactionHistoryToSave = new ArrayList<>();

        for (InvoiceItemDto itemDto : itemDtos) {
            Supplier supplier = supplierCache.computeIfAbsent(
                    itemDto.getSupplierId(),
                    id -> supplierRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Supplier"))
            );

            AccountHead accountHead = accountHeadCache.computeIfAbsent(
                    itemDto.getAccountHeadId(),
                    id -> accountHeadRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Account Head"))
            );

            InvoiceItem.InvoiceItemBuilder builder = InvoiceItem.builder()
                    .invoice(invoice)
                    .supplier(supplier)
                    .accountHead(accountHead)
                    .title(itemDto.getTitle())
                    .document(itemDto.getDocument())
                    .invoiceType(itemDto.getInvoiceType())
                    .quantity(itemDto.getQuantity())
                    .buyPrice(itemDto.getBuyPrice())
                    .sellPrice(itemDto.getSellPrice())
                    .step(itemDto.getStep())
                    .createdBy(authUserId)
                    .createAt(now);

            if (agencyUser != null) {
                builder.agencyUser(agencyUser);
            }

            itemsToSave.add(builder.build());
        }

        // Persist all items in batch
        List<InvoiceItem> savedItems = invoiceItemRepository.saveAll(itemsToSave);

        int count = 0;

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (InvoiceItemDto itemDto : itemDtos) {

            Supplier supplier = supplierCache.computeIfAbsent(
                    itemDto.getSupplierId(),
                    id -> supplierRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Supplier"))
            );

            BigDecimal itemTotalBuyPrice = itemDto.getBuyPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));

            BigDecimal itemTotalSellPrice = itemDto.getSellPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));

            // Revenue = Sell - Buy
            BigDecimal itemRevenue = itemTotalSellPrice.subtract(itemTotalBuyPrice);

            // reassign BigDecimal
            totalRevenue = totalRevenue.add(itemRevenue);

            BigDecimal totalPayableAmount =
                    supplier.getPayableAmount().add(itemTotalBuyPrice);

            SupplierTransactionHistory transactionHistory =
                    SupplierTransactionHistory.builder()
                            .invoiceItemId(savedItems.get(count).getId())
                            .invoiceId(invoice.getId())
                            .agencyId(agencyUser != null ? agencyUser.getId() : null)
                            .ledgerId(invoice.getLedger().getId())
                            .supplierId(itemDto.getSupplierId())
                            .payableAmount(itemTotalBuyPrice)
                            .title(invoice.getInvoiceTitle())
                            .description(invoice.getInvoiceDetails())
                            .createdDate(now)
                            .build();

            transactionHistoryToSave.add(transactionHistory);

            supplier.setPayableAmount(totalPayableAmount);
            supplierRepository.save(supplier);

            count++;
        }

        // reassign again
        invoice.setInvoiceRevenue(invoice.getInvoiceRevenue().add(totalRevenue));

        invoiceRepository.save(invoice);

        // Save supplier transaction history
        supplierTransactionHistoryRepository.saveAll(transactionHistoryToSave);

        // Build dynamic items from saved items
        List<InvoiceDynamicItem> dynamicItems = new ArrayList<>();

        for (int i = 0; i < itemDtos.size(); i++) {
            InvoiceItemDto itemDto = itemDtos.get(i);
            InvoiceItem savedItem = savedItems.get(i);

            Map<String, String> customValues = itemDto.getCustomValues();
            if (customValues == null || customValues.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, String> entry : customValues.entrySet()) {
                dynamicItems.add(
                        InvoiceDynamicItem.builder()
                                .invoiceItem(savedItem)
                                .key(entry.getKey())
                                .value(entry.getValue())
                                .build()
                );
            }
        }

        if (!dynamicItems.isEmpty()) {
            invoiceDynamicItemRepository.saveAll(dynamicItems);
        }
    }

    private InvoiceResponseDto dto(Invoice invoice) {
        InvoiceResponseDto invoiceResponseDto = new InvoiceResponseDto();
        invoiceResponseDto.setId(invoice.getId());
        invoiceResponseDto.setLedgerId(invoice.getLedger().getId());
        invoiceResponseDto.setInvoiceTitle(invoice.getInvoiceTitle());
        invoiceResponseDto.setInvoiceDetails(invoice.getInvoiceDetails());
        invoiceResponseDto.setDocument(invoice.getDocument());
        invoiceResponseDto.setInvoiceDate(invoice.getInvoiceDate());
        invoiceResponseDto.setPaymentMethod(invoice.getPaymentMethod());
        invoiceResponseDto.setInvoiceAmount(invoice.getInvoiceAmount());
        invoiceResponseDto.setInvoiceDiscount(invoice.getInvoiceDiscount());
        invoiceResponseDto.setInvoiceServiceCharge(invoice.getInvoiceServiceCharge());
        invoiceResponseDto.setCreatedBy(invoice.getCreatedBy());
        invoiceResponseDto.setCreatedAt(timestampMapper.toRequestUserTime(invoice.getCreatedAt(), invoice.getCreatedTimeOffset()));
        invoiceResponseDto.setUpdatedBy(invoice.getUpdatedBy());
        invoiceResponseDto.setUpdatedAt(timestampMapper.toRequestUserTime(invoice.getUpdatedAt(), invoice.getUpdatedTimeOffset() != null ? invoice.getUpdatedTimeOffset() : invoice.getCreatedTimeOffset()));
        invoiceResponseDto.setStatus(String.valueOf(invoice.getStatus()));
        if (invoice.getTraveller() != null) {
            invoiceResponseDto.setTravellerId(invoice.getTraveller().getId());
        } else {
            invoiceResponseDto.setTravellerId(null);
        }
        return invoiceResponseDto;
    }

    private InvoiceFullResponseDTO fullDto(Invoice invoice) {
        String provider = invoice.getAgencyUser() == null ? "Admin" : "Agency";

        InvoiceFullResponseDTO invoiceFullResponseDTO = new InvoiceFullResponseDTO();
        invoiceFullResponseDTO.setId(invoice.getId());
        invoiceFullResponseDTO.setLedger(invoice.getLedger());
        invoiceFullResponseDTO.setInvoiceTitle(invoice.getInvoiceTitle());
        invoiceFullResponseDTO.setInvoiceDetails(invoice.getInvoiceDetails());
        invoiceFullResponseDTO.setDocument(invoice.getDocument());
        invoiceFullResponseDTO.setInvoiceAmount(invoice.getInvoiceAmount());
        invoiceFullResponseDTO.setInvoiceServiceCharge(invoice.getInvoiceServiceCharge());
        invoiceFullResponseDTO.setInvoiceDiscount(invoice.getInvoiceDiscount());
        invoiceFullResponseDTO.setPaymentMethod(invoice.getPaymentMethod());
        invoiceFullResponseDTO.setCreatedBy(toCreatorDto(invoice.getCreatedBy(), provider));
        invoiceFullResponseDTO.setUpdatedBy(toCreatorDto(invoice.getUpdatedBy(), provider));
        invoiceFullResponseDTO.setCreatedAt(timestampMapper.toRequestUserTime(invoice.getCreatedAt(), invoice.getCreatedTimeOffset()));
        invoiceFullResponseDTO.setUpdatedAt(timestampMapper.toRequestUserTime(invoice.getUpdatedAt(), invoice.getUpdatedTimeOffset() != null ? invoice.getUpdatedTimeOffset() : invoice.getCreatedTimeOffset()));
        invoiceFullResponseDTO.setStatus(String.valueOf(invoice.getStatus()));

        if (invoice.getTraveller() != null) {
            invoiceFullResponseDTO.setTraveller(invoice.getTraveller());
        } else {
            invoiceFullResponseDTO.setTraveller(null);
        }

        List<InvoiceItemResponseDto> itemDtos = new ArrayList<>();

        for (InvoiceItem item : invoiceItemRepository.findByInvoice(invoice)) {
            InvoiceItemResponseDto itemDto = new InvoiceItemResponseDto();

            itemDto.setId(item.getId());
            itemDto.setTitle(item.getTitle());
            itemDto.setDocument(item.getDocument());
            itemDto.setInvoiceType(item.getInvoiceType());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setSellPrice(item.getSellPrice());
            itemDto.setBuyPrice(item.getBuyPrice());
            itemDto.setStep(item.getStep());
            itemDto.setSupplier(toSupplierDto(item.getSupplier()));
            itemDto.setAccountHead(toAccountHeadDto(item.getAccountHead()));

            if (invoiceDynamicItemRepository.findByInvoiceItemId(item.getId()) != null) {
                Map<String, String> customMap = new HashMap<>();
                for (InvoiceDynamicItem dyn : invoiceDynamicItemRepository.findByInvoiceItemId(item.getId())) {
                    customMap.put(dyn.getKey(), dyn.getValue());
                }
                itemDto.setCustomValues(customMap);
            }

            itemDtos.add(itemDto);
        }

        invoiceFullResponseDTO.setInvoiceItems(itemDtos);

        return invoiceFullResponseDTO;
    }

    private AccountHeadDto toAccountHeadDto(AccountHead entity) {
        if (entity == null) {
            return null;
        }

        AccountHeadDto dto = new AccountHeadDto();

        dto.setId(entity.getId());
        dto.setAccountHeadTitle(entity.getAccountHeadTitle());
        dto.setType(entity.getType() != null ? entity.getType().name() : null);
        dto.setParentId(entity.getParentId());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(timestampMapper.toRequestUserTimeString(entity.getCreatedAt(), entity.getCreatedTimeOffset()));
        dto.setUpdatedAt(timestampMapper.toRequestUserTimeString(entity.getUpdatedAt(), entity.getUpdatedTimeOffset() != null ? entity.getUpdatedTimeOffset() : entity.getCreatedTimeOffset()));
        dto.setUsingPortal(entity.getUsingPortal() != null ? entity.getUsingPortal().name() : null);
        dto.setPortalId(entity.getPortalId());

        return dto;
    }

    private SupplierDto toSupplierDto(Supplier entity) {
        if (entity == null) {
            return null;
        }

        SupplierDto dto = new SupplierDto();

        dto.setId(entity.getId());
        dto.setAgencyId(
                entity.getAgencyUser() != null ? entity.getAgencyUser().getId() : null
        );
        dto.setName(entity.getName());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setEmail(entity.getEmail());
        dto.setAddress(entity.getAddress());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreateAt(timestampMapper.toRequestUserTimeString(entity.getCreateAt(), entity.getCreatedTimeOffset()));
        dto.setUpdateAt(timestampMapper.toRequestUserTimeString(entity.getUpdateAt(), entity.getUpdatedTimeOffset() != null ? entity.getUpdatedTimeOffset() : entity.getCreatedTimeOffset()));
        dto.setIsDeleted(entity.getIsDeleted());

        return dto;
    }

    private CreatorDto toCreatorDto(Long id, String type) {
        if (id == null || type == null) return null;

        if ("Admin".equalsIgnoreCase(type)) {
            AdminUser user = adminUserRepository.findById(id).orElse(null);
            if (user == null) return null;

            CreatorDto dto = new CreatorDto();
            dto.setId(user.getId());
            dto.setFullName(user.getFullName());

            return dto;

        } else {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) return null;

            CreatorDto dto = new CreatorDto();
            dto.setId(user.getId());
            dto.setFullName(user.getFullName());

            return dto;
        }
    }
}
