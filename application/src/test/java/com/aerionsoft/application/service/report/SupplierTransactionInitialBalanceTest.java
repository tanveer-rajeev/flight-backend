package com.aerionsoft.application.service.report;

import com.aerionsoft.application.dto.report.SupplierTransactionReportSummaryDTO;
import com.aerionsoft.application.entity.client.SupplierTransactionHistory;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.repository.booking.TravelInformationRepository;
import com.aerionsoft.application.repository.client.InvoiceItemRepository;
import com.aerionsoft.application.repository.client.InvoiceLedgerRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.repository.client.SupplierTransactionHistoryRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.business.BusinessService;
import com.aerionsoft.application.service.wallet.BankLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SupplierTransactionInitialBalanceTest {

    @Mock
    private SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private InvoiceLedgerRepository invoiceLedgerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BusinessService businessService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TravelInformationRepository travelInformationRepository;
    @Mock
    private BankLedgerService bankLedgerService;

    private SupplierTransactionService supplierTransactionService;

    @BeforeEach
    void setUp() {
        supplierTransactionService = new SupplierTransactionService();
        ReflectionTestUtils.setField(supplierTransactionService, "supplierTransactionHistoryRepository", supplierTransactionHistoryRepository);
        ReflectionTestUtils.setField(supplierTransactionService, "invoiceItemRepository", invoiceItemRepository);
        ReflectionTestUtils.setField(supplierTransactionService, "ledgerRepository", invoiceLedgerRepository);
        ReflectionTestUtils.setField(supplierTransactionService, "supplierRepository", supplierRepository);
        ReflectionTestUtils.setField(supplierTransactionService, "userRepository", userRepository);
        ReflectionTestUtils.setField(supplierTransactionService, "businessService", businessService);
        ReflectionTestUtils.setField(supplierTransactionService, "bookingRepository", bookingRepository);
        ReflectionTestUtils.setField(supplierTransactionService, "travelInformationRepository", travelInformationRepository);
        ReflectionTestUtils.setField(supplierTransactionService, "bankLedgerService", bankLedgerService);
    }

    @Test
    void computeOutstandingBalances_startsFromInitialBalance() {
        SupplierTransactionHistory purchase = history(1L, new BigDecimal("200.0000"), null);
        SupplierTransactionHistory payment = history(2L, null, new BigDecimal("50.0000"));

        @SuppressWarnings("unchecked")
        Map<Long, BigDecimal> balances = ReflectionTestUtils.invokeMethod(
                supplierTransactionService,
                "computeOutstandingBalances",
                List.of(purchase, payment),
                Map.of(),
                new BigDecimal("1000.0000")
        );

        assertEquals(new BigDecimal("1200.0000"), balances.get(1L));
        assertEquals(new BigDecimal("1150.0000"), balances.get(2L));
    }

    @Test
    void computeOutstandingBalances_supportsNegativeInitialBalanceForAdvanceCredit() {
        SupplierTransactionHistory purchase = history(1L, new BigDecimal("100.0000"), null);

        @SuppressWarnings("unchecked")
        Map<Long, BigDecimal> balances = ReflectionTestUtils.invokeMethod(
                supplierTransactionService,
                "computeOutstandingBalances",
                List.of(purchase),
                Map.of(),
                new BigDecimal("-500.0000")
        );

        assertEquals(new BigDecimal("-400.0000"), balances.get(1L));
    }

    @Test
    void buildSummary_includesInitialBalanceInOutstanding() {
        SupplierTransactionReportSummaryDTO summary = ReflectionTestUtils.invokeMethod(
                supplierTransactionService,
                "buildSummary",
                List.of(),
                Map.of(),
                new BigDecimal("800.0000"),
                new BigDecimal("200.0000"),
                new BigDecimal("1000.0000")
        );

        assertEquals(new BigDecimal("1600.0000"), summary.getOutstandingBalance());
    }

    private SupplierTransactionHistory history(Long id, BigDecimal payableAmount, BigDecimal paidAmount) {
        SupplierTransactionHistory history = new SupplierTransactionHistory();
        history.setId(id);
        history.setPayableAmount(payableAmount);
        history.setPaidAmount(paidAmount);
        return history;
    }
}
