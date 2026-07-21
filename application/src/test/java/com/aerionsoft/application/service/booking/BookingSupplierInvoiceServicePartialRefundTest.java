package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.client.Invoice;
import com.aerionsoft.application.entity.client.Supplier;
import com.aerionsoft.application.entity.client.SupplierTransactionHistory;
import com.aerionsoft.application.enums.client.InvoiceStatus;
import com.aerionsoft.application.repository.client.InvoiceRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.repository.client.SupplierTransactionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests what happens to the supplier when an admin issues a PARTIAL refund.
 *
 * After the fix: the supplier payable is reversed proportionally to the refund
 * ratio (refundedAmount / bookingPrice).  A full refund (ratio=1.0) still fully
 * clears the supplier payable; a partial refund leaves the deducted fraction of
 * the buy price outstanding.
 */
@ExtendWith(MockitoExtension.class)
class BookingSupplierInvoiceServicePartialRefundTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;

    private BookingSupplierInvoiceService service;

    @BeforeEach
    void setUp() {
        service = new BookingSupplierInvoiceService(
                invoiceRepository, supplierRepository, supplierTransactionHistoryRepository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Booking booking(Long id, String pnr) {
        Booking b = new Booking();
        b.setId(id);
        b.setPnr(pnr);
        return b;
    }

    private Supplier supplier(Long id, BigDecimal payable) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setPayableAmount(payable);
        return s;
    }

    private Invoice invoice(Long id, Long bookingId) {
        Invoice inv = new Invoice();
        inv.setId(id);
        // invoiceDetails contains " {bookingId} |" so the search matches
        inv.setInvoiceDetails("Booking id " + bookingId + " | PNR ABC");
        inv.setStatus(InvoiceStatus.PENDING);
        return inv;
    }

    private SupplierTransactionHistory txn(Long id, Long invoiceId, Long supplierId, BigDecimal payable) {
        return SupplierTransactionHistory.builder()
                .id(id)
                .invoiceId(invoiceId)
                .supplierId(supplierId)
                .payableAmount(payable)
                .description("Auto-created payable for booking id " + id + " | PNR ABC")
                .build();
    }

    // -------------------------------------------------------------------------
    // Test 1a – FULL refund (ratio=1.0) still wipes the entire supplier payable
    // -------------------------------------------------------------------------
    @Test
    void fullRefund_supplierPayableIsFullyReversed() {
        Long bookingId = 42L;
        Long invoiceId = 10L;
        Long supplierId = 5L;
        BigDecimal buyPrice = new BigDecimal("500.00");
        BigDecimal supplierInitialPayable = new BigDecimal("800.00");

        Booking bk = booking(bookingId, "TEST001");
        Invoice inv = invoice(invoiceId, bookingId);
        Supplier sup = supplier(supplierId, supplierInitialPayable);
        SupplierTransactionHistory original = txn(20L, invoiceId, supplierId, buyPrice);

        when(invoiceRepository.findByInvoiceDetailsContaining(" " + bookingId + " |"))
                .thenReturn(List.of(inv));
        when(supplierTransactionHistoryRepository
                .findByInvoiceIdAndDescriptionNotContaining(invoiceId, "Reversed for refunded booking"))
                .thenReturn(List.of(original));
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(sup));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(inv));

        // Full refund → ratio 1.0
        service.reverseSupplierPayableForRefundedBookingByRatio(bk, BigDecimal.ONE);

        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getPayableAmount())
                .as("Full refund: supplier payable drops by the full buy price")
                .isEqualByComparingTo(new BigDecimal("300.00")); // 800 - 500

        ArgumentCaptor<SupplierTransactionHistory> txnCaptor =
                ArgumentCaptor.forClass(SupplierTransactionHistory.class);
        verify(supplierTransactionHistoryRepository).save(txnCaptor.capture());
        assertThat(txnCaptor.getValue().getPayableAmount())
                .isEqualByComparingTo(buyPrice.negate()); // -500

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getStatus()).isEqualTo(InvoiceStatus.REJECTED);
    }

    // -------------------------------------------------------------------------
    // Test 1b – PARTIAL refund (ratio=0.8) reverses only 80% of supplier payable
    // -------------------------------------------------------------------------
    @Test
    void partialRefund_supplierPayableIsReversedProportionally() {
        Long bookingId = 43L;
        Long invoiceId = 11L;
        Long supplierId = 6L;
        // Booking: sell price 500, deduction 100, refunded 400 → ratio = 400/500 = 0.8
        BigDecimal buyPrice = new BigDecimal("500.00");
        BigDecimal supplierInitialPayable = new BigDecimal("800.00");
        BigDecimal refundRatio = new BigDecimal("0.8");

        Booking bk = booking(bookingId, "TEST002");
        Invoice inv = invoice(invoiceId, bookingId);
        Supplier sup = supplier(supplierId, supplierInitialPayable);
        SupplierTransactionHistory original = txn(21L, invoiceId, supplierId, buyPrice);

        when(invoiceRepository.findByInvoiceDetailsContaining(" " + bookingId + " |"))
                .thenReturn(List.of(inv));
        when(supplierTransactionHistoryRepository
                .findByInvoiceIdAndDescriptionNotContaining(invoiceId, "Reversed for refunded booking"))
                .thenReturn(List.of(original));
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(sup));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(inv));

        service.reverseSupplierPayableForRefundedBookingByRatio(bk, refundRatio);

        // Supplier payable: 800 - (500 × 0.8) = 800 - 400 = 400
        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getPayableAmount())
                .as("Partial refund (80%): supplier payable reduced by 80% of buy price only")
                .isEqualByComparingTo(new BigDecimal("400.0000")); // 800 - 400

        // Reversal entry carries -400 (not -500)
        ArgumentCaptor<SupplierTransactionHistory> txnCaptor =
                ArgumentCaptor.forClass(SupplierTransactionHistory.class);
        verify(supplierTransactionHistoryRepository).save(txnCaptor.capture());
        SupplierTransactionHistory reversal = txnCaptor.getValue();
        assertThat(reversal.getPayableAmount())
                .as("Reversal amount must be proportional: -(500 × 0.8) = -400")
                .isEqualByComparingTo(new BigDecimal("-400.0000"));

        assertThat(reversal.getDescription())
                .contains("Reversed for refunded booking")
                .contains(String.valueOf(bookingId))
                .contains("0.8");

        assertThat(reversal.getTitle()).contains("TEST002");

        // Invoice is still marked REJECTED (booking is cancelled regardless)
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getStatus()).isEqualTo(InvoiceStatus.REJECTED);
    }

    // -------------------------------------------------------------------------
    // Test 2 – supplier payable is floored at zero (never goes negative)
    // -------------------------------------------------------------------------
    @Test
    void fullRefund_supplierPayableIsFlooredAtZero_ifOriginalExceedsCurrentBalance() {
        Long bookingId = 99L;
        Long invoiceId = 12L;
        Long supplierId = 7L;
        // Buy price is larger than the supplier's current payable (data inconsistency)
        BigDecimal buyPrice = new BigDecimal("1000.00");
        BigDecimal supplierCurrentPayable = new BigDecimal("200.00");

        Booking bk = booking(bookingId, "LOWBAL");
        Invoice inv = invoice(invoiceId, bookingId);
        Supplier sup = supplier(supplierId, supplierCurrentPayable);
        SupplierTransactionHistory original = txn(30L, invoiceId, supplierId, buyPrice);

        when(invoiceRepository.findByInvoiceDetailsContaining(" " + bookingId + " |"))
                .thenReturn(List.of(inv));
        when(supplierTransactionHistoryRepository
                .findByInvoiceIdAndDescriptionNotContaining(invoiceId, "Reversed for refunded booking"))
                .thenReturn(List.of(original));
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(sup));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(inv));

        // Full reversal → ratio 1.0 → would try to subtract 1000 from 200
        service.reverseSupplierPayableForRefundedBookingByRatio(bk, BigDecimal.ONE);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertThat(captor.getValue().getPayableAmount())
                .as("Supplier payable must not go below zero")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // Test 3 – second call (idempotency): already-reversed transactions are skipped
    // -------------------------------------------------------------------------
    @Test
    void reversal_calledTwice_doesNotDoubleReverseSupplier() {
        Long bookingId = 55L;
        Long invoiceId = 13L;

        Booking bk = booking(bookingId, "IDEM001");
        Invoice inv = invoice(invoiceId, bookingId);

        when(invoiceRepository.findByInvoiceDetailsContaining(" " + bookingId + " |"))
                .thenReturn(List.of(inv));
        // Second call: all transactions already carry the reversal marker → filtered out
        when(supplierTransactionHistoryRepository
                .findByInvoiceIdAndDescriptionNotContaining(invoiceId, "Reversed for refunded booking"))
                .thenReturn(List.of());

        service.reverseSupplierPayableForRefundedBookingByRatio(bk, BigDecimal.ONE);

        // Supplier and transaction repo must not be touched
        verify(supplierRepository, never()).save(any());
        verify(supplierTransactionHistoryRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Test 4a – no invoice → fallback to description search, full refund
    // -------------------------------------------------------------------------
    @Test
    void fullRefund_noInvoiceFound_fallsBackToDescriptionSearch_andStillReversesSupplier() {
        Long bookingId = 77L;
        Long supplierId = 9L;
        BigDecimal buyPrice = new BigDecimal("300.00");
        BigDecimal supplierPayable = new BigDecimal("600.00");

        Booking bk = booking(bookingId, "NOINV");
        Supplier sup = supplier(supplierId, supplierPayable);

        when(invoiceRepository.findByInvoiceDetailsContaining(" " + bookingId + " |"))
                .thenReturn(List.of());

        SupplierTransactionHistory original = SupplierTransactionHistory.builder()
                .id(40L)
                .invoiceId(null)
                .supplierId(supplierId)
                .payableAmount(buyPrice)
                .description("Auto-created payable for booking id " + bookingId + " | NOINV")
                .build();

        when(supplierTransactionHistoryRepository.findByDescriptionContainingAndDescriptionNotContaining(
                " id " + bookingId + " |", "Reversed for refunded booking"))
                .thenReturn(List.of(original));

        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(sup));

        service.reverseSupplierPayableForRefundedBookingByRatio(bk, BigDecimal.ONE);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertThat(captor.getValue().getPayableAmount())
                .as("Full refund via fallback: 600 - 300 = 300")
                .isEqualByComparingTo(new BigDecimal("300.00"));

        verify(invoiceRepository, never()).findById(any());
    }

    // -------------------------------------------------------------------------
    // Test 4b – no invoice → fallback, partial refund (50%)
    // -------------------------------------------------------------------------
    @Test
    void partialRefund_noInvoiceFound_fallsBackToDescriptionSearch_reversesProportionally() {
        Long bookingId = 78L;
        Long supplierId = 10L;
        BigDecimal buyPrice = new BigDecimal("400.00");
        BigDecimal supplierPayable = new BigDecimal("700.00");
        BigDecimal refundRatio = new BigDecimal("0.5"); // 50% refunded

        Booking bk = booking(bookingId, "NOINV2");
        Supplier sup = supplier(supplierId, supplierPayable);

        when(invoiceRepository.findByInvoiceDetailsContaining(" " + bookingId + " |"))
                .thenReturn(List.of());

        SupplierTransactionHistory original = SupplierTransactionHistory.builder()
                .id(41L)
                .invoiceId(null)
                .supplierId(supplierId)
                .payableAmount(buyPrice)
                .description("Auto-created payable for booking id " + bookingId + " | NOINV2")
                .build();

        when(supplierTransactionHistoryRepository.findByDescriptionContainingAndDescriptionNotContaining(
                " id " + bookingId + " |", "Reversed for refunded booking"))
                .thenReturn(List.of(original));

        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(sup));

        service.reverseSupplierPayableForRefundedBookingByRatio(bk, refundRatio);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertThat(captor.getValue().getPayableAmount())
                .as("50% refund: 700 - (400 × 0.5) = 700 - 200 = 500")
                .isEqualByComparingTo(new BigDecimal("500.0000"));
    }

    // -------------------------------------------------------------------------
    // Test 6 – explicit supplier refund cost (admin refund)
    // Buy 900, supplier keeps 200 → reverse 700, remaining payable 200
    // -------------------------------------------------------------------------
    @Test
    void supplierRefundCost_reversesBuyPriceMinusSupplierCost() {
        Long bookingId = 101L;
        Long invoiceId = 20L;
        Long supplierId = 11L;
        BigDecimal buyPrice = new BigDecimal("900.00");
        BigDecimal supplierRefundCost = new BigDecimal("200.00");
        BigDecimal supplierInitialPayable = new BigDecimal("1500.00");

        Booking bk = booking(bookingId, "SUP200");
        Invoice inv = invoice(invoiceId, bookingId);
        Supplier sup = supplier(supplierId, supplierInitialPayable);
        SupplierTransactionHistory original = txn(50L, invoiceId, supplierId, buyPrice);

        when(invoiceRepository.findByInvoiceDetailsContaining(" " + bookingId + " |"))
                .thenReturn(List.of(inv));
        when(supplierTransactionHistoryRepository
                .findByInvoiceIdAndDescriptionNotContaining(invoiceId, "Reversed for refunded booking"))
                .thenReturn(List.of(original));
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(sup));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(inv));

        service.reverseSupplierPayableForRefundedBooking(bk, supplierRefundCost);

        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getPayableAmount())
                .as("Supplier payable reduced by buyPrice - supplierRefundCost = 700")
                .isEqualByComparingTo(new BigDecimal("800.00"));

        ArgumentCaptor<SupplierTransactionHistory> txnCaptor =
                ArgumentCaptor.forClass(SupplierTransactionHistory.class);
        verify(supplierTransactionHistoryRepository).save(txnCaptor.capture());
        assertThat(txnCaptor.getValue().getPayableAmount())
                .isEqualByComparingTo(new BigDecimal("-700.00"));
        assertThat(txnCaptor.getValue().getDescription())
                .contains("supplierRefundCost 200.00");
    }

    @Test
    void supplierRefundCost_zero_reversesFullBuyPrice() {
        Long bookingId = 102L;
        Long invoiceId = 21L;
        Long supplierId = 12L;
        BigDecimal buyPrice = new BigDecimal("900.00");
        BigDecimal supplierInitialPayable = new BigDecimal("900.00");

        Booking bk = booking(bookingId, "SUP0");
        Invoice inv = invoice(invoiceId, bookingId);
        Supplier sup = supplier(supplierId, supplierInitialPayable);
        SupplierTransactionHistory original = txn(51L, invoiceId, supplierId, buyPrice);

        when(invoiceRepository.findByInvoiceDetailsContaining(" " + bookingId + " |"))
                .thenReturn(List.of(inv));
        when(supplierTransactionHistoryRepository
                .findByInvoiceIdAndDescriptionNotContaining(invoiceId, "Reversed for refunded booking"))
                .thenReturn(List.of(original));
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(sup));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(inv));

        service.reverseSupplierPayableForRefundedBooking(bk, BigDecimal.ZERO);

        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getPayableAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // Test 5 – null booking / null ID: service exits gracefully
    // -------------------------------------------------------------------------
    @Test
    void reverseSupplierPayable_withNullBooking_doesNothing() {
        service.reverseSupplierPayableForRefundedBooking(null);
        verifyNoInteractions(invoiceRepository, supplierRepository, supplierTransactionHistoryRepository);
    }

    @Test
    void reverseSupplierPayable_withNullBookingId_doesNothing() {
        Booking bk = new Booking(); // id is null
        service.reverseSupplierPayableForRefundedBooking(bk);
        verifyNoInteractions(invoiceRepository, supplierRepository, supplierTransactionHistoryRepository);
    }
}
