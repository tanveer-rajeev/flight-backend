package com.aerionsoft.application.service.client;

import com.aerionsoft.application.dto.client.invoice.SupplierDto;
import com.aerionsoft.application.entity.client.Branch;
import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.repository.client.InvoiceDynamicItemRepository;
import com.aerionsoft.application.repository.client.InvoiceItemRepository;
import com.aerionsoft.application.repository.client.InvoiceRepository;
import com.aerionsoft.application.repository.client.SupplierProviderMappingRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.repository.client.SupplierTransactionHistoryRepository;
import com.aerionsoft.application.repository.group.GroupTicketRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.common.PlatformProviderService;
import com.aerionsoft.application.util.TimestampMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierInitialBalanceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BranchService branchService;
    @Mock
    private TimestampMapper timestampMapper;
    @Mock
    private SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private InvoiceDynamicItemRepository invoiceDynamicItemRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private GroupTicketRepository groupTicketRepository;
    @Mock
    private SupplierProviderMappingRepository mappingRepository;
    @Mock
    private PlatformProviderService platformProviderService;

    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierService(
                supplierRepository,
                userRepository,
                branchService,
                timestampMapper,
                supplierTransactionHistoryRepository,
                invoiceItemRepository,
                invoiceDynamicItemRepository,
                invoiceRepository,
                groupTicketRepository,
                mappingRepository,
                platformProviderService
        );
    }

    @Test
    void createSupplier_storesInitialBalanceWithoutChangingPayableOrPaid() {
        Branch branch = new Branch();
        branch.setId(1L);
        when(branchService.resolveBranchForSupplier(eq("admin"), eq(99L), eq(null))).thenReturn(branch);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierDto dto = buildSupplierDto(new BigDecimal("5000.0000"));

        supplierService.createSupplier("admin", 99L, dto);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());

        Supplier saved = captor.getValue();
        assertEquals(new BigDecimal("5000.0000"), saved.getInitialBalance());
        assertEquals(BigDecimal.ZERO, saved.getPayableAmount());
        assertEquals(BigDecimal.ZERO, saved.getPaidAmount());
    }

    @Test
    void createSupplier_defaultsInitialBalanceToZeroWhenNull() {
        Branch branch = new Branch();
        branch.setId(1L);
        when(branchService.resolveBranchForSupplier(eq("admin"), eq(99L), eq(null))).thenReturn(branch);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierDto dto = buildSupplierDto(null);

        supplierService.createSupplier("admin", 99L, dto);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());

        assertEquals(BigDecimal.ZERO, captor.getValue().getInitialBalance());
    }

    @Test
    void updateSupplier_changesInitialBalanceWhenProvided() {
        Supplier supplier = existingSupplier(new BigDecimal("1000.0000"));
        when(supplierRepository.findByIdAndAgencyUserIsNull(5L)).thenReturn(Optional.of(supplier));
        when(branchService.resolveBranchForSupplier(eq("admin"), eq(99L), eq(null))).thenReturn(supplier.getBranch());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierDto dto = buildSupplierDto(new BigDecimal("2500.5000"));

        supplierService.updateSupplier("admin", 99L, 5L, dto);

        assertEquals(new BigDecimal("2500.5000"), supplier.getInitialBalance());
    }

    @Test
    void updateSupplier_keepsInitialBalanceWhenFieldIsNull() {
        Supplier supplier = existingSupplier(new BigDecimal("1000.0000"));
        when(supplierRepository.findByIdAndAgencyUserIsNull(5L)).thenReturn(Optional.of(supplier));
        when(branchService.resolveBranchForSupplier(eq("admin"), eq(99L), eq(null))).thenReturn(supplier.getBranch());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierDto dto = buildSupplierDto(null);

        supplierService.updateSupplier("admin", 99L, 5L, dto);

        assertEquals(new BigDecimal("1000.0000"), supplier.getInitialBalance());
    }

    private SupplierDto buildSupplierDto(BigDecimal initialBalance) {
        SupplierDto dto = new SupplierDto();
        dto.setName("Test Supplier");
        dto.setEmail("supplier@test.com");
        dto.setPhoneNumber("1234567890");
        dto.setAddress("Test Address");
        dto.setInitialBalance(initialBalance);
        return dto;
    }

    private Supplier existingSupplier(BigDecimal initialBalance) {
        Branch branch = new Branch();
        branch.setId(1L);

        Supplier supplier = new Supplier();
        supplier.setId(5L);
        supplier.setBranch(branch);
        supplier.setName("Old Name");
        supplier.setEmail("old@test.com");
        supplier.setPhoneNumber("1234567890");
        supplier.setAddress("Old Address");
        supplier.setInitialBalance(initialBalance);
        supplier.setPayableAmount(new BigDecimal("300.0000"));
        supplier.setPaidAmount(new BigDecimal("100.0000"));
        return supplier;
    }
}
