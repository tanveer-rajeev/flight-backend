package com.aerionsoft.application.service.client;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.client.branch.BranchResponseDto;
import com.aerionsoft.application.dto.client.invoice.SupplierBulkAssignBranchRequest;
import com.aerionsoft.application.dto.client.invoice.SupplierDto;
import com.aerionsoft.application.dto.client.invoice.SupplierProviderMappingDto;
import com.aerionsoft.application.dto.client.invoice.response.SupplierBulkAssignBranchResponse;
import com.aerionsoft.application.dto.client.invoice.response.SupplierResponseDto;
import com.aerionsoft.application.dto.platform.PlatformProviderChannel;
import com.aerionsoft.application.entity.client.Branch;
import com.aerionsoft.application.entity.client.Invoice;
import com.aerionsoft.application.entity.client.InvoiceItem;
import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.entity.client.SupplierProviderMapping;
import com.aerionsoft.application.entity.client.SupplierTransactionHistory;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.group.GroupTicket;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.enums.client.ManualInvoicePaymentType;
import com.aerionsoft.application.repository.client.InvoiceDynamicItemRepository;
import com.aerionsoft.application.repository.client.InvoiceItemRepository;
import com.aerionsoft.application.repository.client.InvoiceRepository;
import com.aerionsoft.application.repository.client.SupplierProviderMappingRepository;
import com.aerionsoft.application.repository.client.SupplierTransactionHistoryRepository;
import com.aerionsoft.application.repository.group.GroupTicketRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.service.common.PlatformProviderService;
import com.aerionsoft.application.util.TimestampMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final BranchService branchService;
    private final TimestampMapper timestampMapper;
    private final SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceDynamicItemRepository invoiceDynamicItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final GroupTicketRepository groupTicketRepository;
    private final SupplierProviderMappingRepository mappingRepository;
    private final PlatformProviderService platformProviderService;

    public SupplierService(
            SupplierRepository supplierRepository,
            UserRepository userRepository,
            BranchService branchService,
            TimestampMapper timestampMapper,
            SupplierTransactionHistoryRepository supplierTransactionHistoryRepository,
            InvoiceItemRepository invoiceItemRepository,
            InvoiceDynamicItemRepository invoiceDynamicItemRepository,
            InvoiceRepository invoiceRepository,
            GroupTicketRepository groupTicketRepository,
            SupplierProviderMappingRepository mappingRepository,
            PlatformProviderService platformProviderService
    ) {
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.branchService = branchService;
        this.timestampMapper = timestampMapper;
        this.supplierTransactionHistoryRepository = supplierTransactionHistoryRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceDynamicItemRepository = invoiceDynamicItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.groupTicketRepository = groupTicketRepository;
        this.mappingRepository = mappingRepository;
        this.platformProviderService = platformProviderService;
    }

    /**
     * Supplier list service
     *
     * @param provider   admin or agency
     * @param authUserId authenticate user id
     * @return list or suppliers
     */
    @Transactional(readOnly = true)
    public List<SupplierResponseDto> getAllSuppliers(String provider, Long authUserId, Long branchId) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (branchId != null) {
            branchService.ensureBranchAccessible(provider, authUserId, branchId);
        }

        List<Supplier> suppliers;
        if (isAdmin) {
            suppliers = branchId != null
                    ? supplierRepository.findAllByAgencyUserIsNullAndBranch_Id(branchId)
                    : supplierRepository.findAllByAgencyUserIsNull();
        } else {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
            suppliers = branchId != null
                    ? supplierRepository.findAllByAgencyUserAndBranch_Id(agencyUser, branchId)
                    : supplierRepository.findAllByAgencyUser(agencyUser);
        }

        return suppliers.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupplierResponseDto> getSuppliersByBranchId(String provider, Long authUserId, Long branchId) {
        return getAllSuppliers(provider, authUserId, branchId);
    }

    /**
     * Geet supplier by id service
     *
     * @param provider   admin or agency
     * @param authUserId authenticate user id
     * @param id         specific supplier id
     * @return specific supplier
     */
    @Transactional(readOnly = true)
    public SupplierResponseDto getSupplierById(String provider, Long authUserId, Long id) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (isAdmin) {
            Supplier supplier = supplierRepository.findByIdAndAgencyUserIsNull(id).orElseThrow(() -> new ResourceNotFoundException("Supplier"));

            return toDto(supplier);
        }

        User user = userRepository.findById(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;

        Supplier supplier = supplierRepository.findByIdAndAgencyUser(id, agencyUser).orElseThrow(() -> new ResourceNotFoundException("Supplier"));

        return toDto(supplier);
    }

    /**
     * Cached platform provider/channel pairs available for supplier mapping (from startup API load).
     */
    @Transactional(readOnly = true)
    public List<SupplierProviderMappingDto> getPlatformProviderOptions() {
        List<SupplierProviderMappingDto> options = new ArrayList<>(platformProviderService.getAllChannelEntries().stream()
                .map(this::toMappingDto)
                .toList());

        SupplierProviderMappingDto others = new SupplierProviderMappingDto();
        others.setProvider(Provider.OTHERS);
        others.setChannel(null);
        options.add(others);
        return options;
    }

    /**
     * Supplier create service
     *
     * @param provider    admin or agency
     * @param authUserId  authenticate user id
     * @param supplierDto request data to create supplier
     */
    @Transactional
    public void createSupplier(String provider, Long authUserId, SupplierDto supplierDto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (isAdmin) {
            Branch branch = branchService.resolveBranchForSupplier(provider, authUserId, supplierDto.getBranchId());
            Supplier supplier = Supplier.builder()
                    .branch(branch)
                    .name(supplierDto.getName())
                    .title(supplierDto.getTitle())
                    .email(supplierDto.getEmail())
                    .phoneNumber(supplierDto.getPhoneNumber())
                    .address(supplierDto.getAddress())
                    .createdBy(authUserId)
                    .description(supplierDto.getDescription())
                    .createAt(UserDateTimeUtil.now())
                    .paidAmount(BigDecimal.ZERO)
                    .payableAmount(BigDecimal.ZERO)
                    .initialBalance(normalizeInitialBalance(supplierDto.getInitialBalance()))
                    .isDeleted(false)
                    .build();

            supplierRepository.save(supplier);
            applyProviderMappings(supplier, supplierDto.getProviderMappings());
        } else {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
            Branch branch = branchService.resolveBranchForSupplier(provider, authUserId, supplierDto.getBranchId());

            Supplier supplier = Supplier.builder()
                    .agencyUser(agencyUser)
                    .branch(branch)
                    .name(supplierDto.getName())
                    .title(supplierDto.getTitle())
                    .email(supplierDto.getEmail())
                    .phoneNumber(supplierDto.getPhoneNumber())
                    .address(supplierDto.getAddress())
                    .createdBy(authUserId)
                    .description(supplierDto.getDescription())
                    .createAt(UserDateTimeUtil.now())
                    .paidAmount(BigDecimal.ZERO)
                    .payableAmount(BigDecimal.ZERO)
                    .initialBalance(normalizeInitialBalance(supplierDto.getInitialBalance()))
                    .isDeleted(false)
                    .build();

            supplierRepository.save(supplier);
        }
    }

    /**
     * Assigns the same branch to multiple suppliers in one request.
     * Pass {@code branchId: null} to remove branch assignment from all listed suppliers.
     * Operation is all-or-nothing: fails if any supplier id is missing or out of scope.
     */
    @Transactional
    public SupplierBulkAssignBranchResponse bulkAssignBranch(
            String provider,
            Long authUserId,
            SupplierBulkAssignBranchRequest request
    ) {
        List<Long> supplierIds = request.getSupplierIds().stream().distinct().toList();
        Branch branch = branchService.resolveBranchForSupplier(provider, authUserId, request.getBranchId());

        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        List<Supplier> suppliers;
        if (isAdmin) {
            suppliers = supplierRepository.findByIdInAndAgencyUserIsNull(supplierIds);
        } else {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
            suppliers = supplierRepository.findByIdInAndAgencyUser(supplierIds, agencyUser);
        }

        if (suppliers.size() != supplierIds.size()) {
            Set<Long> foundIds = suppliers.stream().map(Supplier::getId).collect(Collectors.toSet());
            List<Long> missingIds = supplierIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Supplier(s) not found or not accessible: " + missingIds);
        }

        LocalDateTime now = UserDateTimeUtil.now();
        for (Supplier supplier : suppliers) {
            supplier.setBranch(branch);
            supplier.setUpdateAt(now);
            supplier.setUpdatedBy(authUserId);
        }
        supplierRepository.saveAll(suppliers);

        List<SupplierResponseDto> updated = suppliers.stream().map(this::toDto).collect(Collectors.toList());
        return new SupplierBulkAssignBranchResponse(
                branch != null ? branch.getId() : null,
                updated.size(),
                updated
        );
    }

    /**
     * Supplier update service
     *
     * @param provider    admin or agency
     * @param authUserId  authenticate user id
     * @param id          to update specific supplier
     * @param supplierDto update request data
     */
    @Transactional
    public void updateSupplier(String provider, Long authUserId, Long id, SupplierDto supplierDto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        if (isAdmin) {
            Supplier supplier = supplierRepository.findByIdAndAgencyUserIsNull(id).orElseThrow(() -> new ResourceNotFoundException("Supplier"));
            Branch branch = branchService.resolveBranchForSupplier(provider, authUserId, supplierDto.getBranchId());

            supplier.setName(supplierDto.getName());
            supplier.setTitle(supplierDto.getTitle());
            supplier.setEmail(supplierDto.getEmail());
            supplier.setPhoneNumber(supplierDto.getPhoneNumber());
            supplier.setAddress(supplierDto.getAddress());
            supplier.setDescription(supplierDto.getDescription());
            supplier.setBranch(branch);
            applyInitialBalanceUpdate(supplier, supplierDto.getInitialBalance());
            supplier.setUpdateAt(UserDateTimeUtil.now());
            supplier.setUpdatedBy(authUserId);

            supplierRepository.save(supplier);
            if (supplierDto.getProviderMappings() != null) {
                applyProviderMappings(supplier, supplierDto.getProviderMappings());
            }
        } else {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;

            Supplier supplier = supplierRepository.findByIdAndAgencyUser(id, agencyUser).orElseThrow(() -> new ResourceNotFoundException("Supplier"));
            Branch branch = branchService.resolveBranchForSupplier(provider, authUserId, supplierDto.getBranchId());

            supplier.setName(supplierDto.getName());
            supplier.setTitle(supplierDto.getTitle());
            supplier.setEmail(supplierDto.getEmail());
            supplier.setPhoneNumber(supplierDto.getPhoneNumber());
            supplier.setAddress(supplierDto.getAddress());
            supplier.setDescription(supplierDto.getDescription());
            supplier.setBranch(branch);
            applyInitialBalanceUpdate(supplier, supplierDto.getInitialBalance());
            supplier.setUpdateAt(UserDateTimeUtil.now());
            supplier.setUpdatedBy(authUserId);

            supplierRepository.save(supplier);
        }
    }

    /**
     * Supplier delete service — soft-deletes the supplier and removes associated
     * invoice items, non-bank transaction histories, and group-ticket links.
     * Bank payments and bank ledger entries are preserved.
     *
     * @param provider   admin or agency
     * @param authUserId authenticate user id
     * @param id         delete specific id
     */
    @Transactional
    public void deleteSupplier(String provider, Long authUserId, Long id) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);

        Supplier supplier;
        if (isAdmin) {
            supplier = supplierRepository.findByIdAndAgencyUserIsNull(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier"));
        } else {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            User agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
            supplier = supplierRepository.findByIdAndAgencyUser(id, agencyUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier"));
        }

        deleteSupplierAssociations(supplier);

        supplier.setIsDeleted(true);
        supplier.setPaidAmount(BigDecimal.ZERO);
        supplier.setPayableAmount(BigDecimal.ZERO);
        supplier.setInitialBalance(BigDecimal.ZERO);
        mappingRepository.deleteBySupplierId(supplier.getId());
        supplier.setUpdateAt(UserDateTimeUtil.now());
        supplier.setUpdatedBy(authUserId);
        supplierRepository.save(supplier);
    }

    private void deleteSupplierAssociations(Supplier supplier) {
        unlinkGroupTickets(supplier);
        deleteSupplierInvoiceItems(supplier);
        deleteNonBankTransactionHistories(supplier.getId());
    }

    private void unlinkGroupTickets(Supplier supplier) {
        List<GroupTicket> tickets = groupTicketRepository.findBySupplier(supplier);
        for (GroupTicket ticket : tickets) {
            ticket.setSupplier(null);
        }
        if (!tickets.isEmpty()) {
            groupTicketRepository.saveAll(tickets);
        }
    }

    private void deleteSupplierInvoiceItems(Supplier supplier) {
        List<InvoiceItem> items = invoiceItemRepository.findBySupplier(supplier);
        if (items.isEmpty()) {
            return;
        }

        Set<Invoice> invoicesToUpdate = new HashSet<>();
        for (InvoiceItem item : items) {
            Invoice invoice = item.getInvoice();
            if (invoice != null) {
                subtractItemFromInvoiceTotals(invoice, item);
                invoicesToUpdate.add(invoice);
            }
        }

        invoiceDynamicItemRepository.deleteByInvoiceItemIn(items);
        invoiceItemRepository.deleteAll(items);

        for (Invoice invoice : invoicesToUpdate) {
            List<InvoiceItem> remaining = invoiceItemRepository.findByInvoiceId(invoice.getId());
            if (remaining.isEmpty()) {
                invoiceRepository.delete(invoice);
            } else {
                invoiceRepository.save(invoice);
            }
        }
    }

    private void subtractItemFromInvoiceTotals(Invoice invoice, InvoiceItem item) {
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
        BigDecimal sellTotal = item.getSellPrice().multiply(quantity);
        BigDecimal buyTotal = item.getBuyPrice().multiply(quantity);
        BigDecimal revenue = sellTotal.subtract(buyTotal);

        BigDecimal invoiceAmount = invoice.getInvoiceAmount() != null ? invoice.getInvoiceAmount() : BigDecimal.ZERO;
        BigDecimal invoiceRevenue = invoice.getInvoiceRevenue() != null ? invoice.getInvoiceRevenue() : BigDecimal.ZERO;

        invoice.setInvoiceAmount(invoiceAmount.subtract(sellTotal));
        invoice.setInvoiceRevenue(invoiceRevenue.subtract(revenue));
    }

    private void deleteNonBankTransactionHistories(Long supplierId) {
        List<SupplierTransactionHistory> histories = supplierTransactionHistoryRepository.findBySupplierId(supplierId);
        List<SupplierTransactionHistory> toDelete = histories.stream()
                .filter(history -> history.getPaymentType() != ManualInvoicePaymentType.BANK)
                .collect(Collectors.toCollection(ArrayList::new));

        if (!toDelete.isEmpty()) {
            supplierTransactionHistoryRepository.deleteAll(toDelete);
        }
    }

    private SupplierResponseDto toDto(Supplier supplier) {
        SupplierResponseDto dto = new SupplierResponseDto();
        dto.setId(supplier.getId());
        if (supplier.getBranch() != null) {
            dto.setBranchId(supplier.getBranch().getId());
            BranchResponseDto branchDto = new BranchResponseDto();
            branchDto.setId(supplier.getBranch().getId());
            branchDto.setName(supplier.getBranch().getName());
            branchDto.setCurrency(supplier.getBranch().getCurrency());
            dto.setBranch(branchDto);
        }
        dto.setName(supplier.getName());
        dto.setTitle(supplier.getTitle());
        dto.setAddress(supplier.getAddress());
        dto.setDescription(supplier.getDescription());
        dto.setEmail(supplier.getEmail());
        dto.setPhoneNumber(supplier.getPhoneNumber());
        dto.setCreatedAt(timestampMapper.toRequestUserTime(supplier.getCreateAt(), supplier.getCreatedTimeOffset()));
        dto.setUpdatedAt(timestampMapper.toRequestUserTime(supplier.getUpdateAt(), supplier.getUpdatedTimeOffset() != null ? supplier.getUpdatedTimeOffset() : supplier.getCreatedTimeOffset()));
        dto.setCreatedBy(supplier.getCreatedBy());
        dto.setUpdatedBy(supplier.getUpdatedBy());
        dto.setIsDeleted(supplier.getIsDeleted());
        dto.setPaidAMount(supplier.getPaidAmount());
        dto.setPayableAMount(supplier.getPayableAmount());
        dto.setInitialBalance(supplier.getInitialBalance());
        dto.setProviderMappings(loadMappingDtos(supplier.getId()));
        return dto;
    }

    private List<SupplierProviderMappingDto> loadMappingDtos(Long supplierId) {
        return mappingRepository.findBySupplierId(supplierId).stream()
                .map(this::toMappingDto)
                .toList();
    }

    private SupplierProviderMappingDto toMappingDto(SupplierProviderMapping mapping) {
        SupplierProviderMappingDto dto = new SupplierProviderMappingDto();
        dto.setProvider(mapping.getProvider());
        dto.setChannel(mapping.getChannel());
        return dto;
    }

    private SupplierProviderMappingDto toMappingDto(PlatformProviderChannel entry) {
        SupplierProviderMappingDto dto = new SupplierProviderMappingDto();
        dto.setProvider(entry.provider());
        dto.setChannel(entry.channel());
        return dto;
    }

    private void applyProviderMappings(Supplier supplier, List<SupplierProviderMappingDto> mappings) {
        mappingRepository.deleteBySupplierId(supplier.getId());
        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        Set<String> platformKeys = platformProviderService.getAllChannelEntries().stream()
                .map(entry -> mappingKey(entry.provider(), entry.channel()))
                .collect(Collectors.toSet());

        for (SupplierProviderMappingDto dto : mappings) {
            if (dto.getProvider() == null) {
                continue;
            }

            String channel = normalizeMappingChannel(dto.getChannel());
            if (channel != null) {
                if (!platformKeys.contains(mappingKey(dto.getProvider(), channel))) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Invalid provider mapping: " + dto.getProvider() + ": " + channel);
                }
                if (mappingRepository.existsByProviderAndChannelAndSupplierIdNot(
                        dto.getProvider(), channel, supplier.getId())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Provider mapping already assigned to another supplier: "
                                    + dto.getProvider() + ": " + channel);
                }
            } else {
                if (mappingRepository.existsByProviderAndChannelIsNullAndSupplierIdNot(
                        dto.getProvider(), supplier.getId())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Provider-wide mapping already assigned to another supplier: " + dto.getProvider());
                }
            }

            SupplierProviderMapping mapping = SupplierProviderMapping.builder()
                    .supplier(supplier)
                    .provider(dto.getProvider())
                    .channel(channel)
                    .build();
            mappingRepository.save(mapping);
        }
    }

    private String normalizeMappingChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        return channel.trim();
    }

    private String mappingKey(Provider provider, String channel) {
        return provider.name() + ":" + channel;
    }

    private BigDecimal normalizeInitialBalance(BigDecimal initialBalance) {
        return initialBalance != null ? initialBalance : BigDecimal.ZERO;
    }

    private void applyInitialBalanceUpdate(Supplier supplier, BigDecimal initialBalance) {
        if (initialBalance != null) {
            supplier.setInitialBalance(initialBalance);
        }
    }
}
