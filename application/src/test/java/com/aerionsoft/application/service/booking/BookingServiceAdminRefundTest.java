package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.dto.booking.AdminBookingRefundRequest;
import com.aerionsoft.application.dto.booking.AdminBookingRefundResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.interafces.UserInterface;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.audit.ActivityBookingAuditSupport;
import com.aerionsoft.application.service.notification.NotificationHelper;
import com.aerionsoft.application.service.wallet.ReferenceGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Admin refund is append-only: credits wallet, reverses supplier, audits —
 * never deletes the original PURCHASE. Supplier failure must propagate so the
 * surrounding @Transactional rolls back wallet + status.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceAdminRefundTest {

    private static final Long BOOKING_ID = 42L;
    private static final Long USER_ID = 10L;

    @Mock private BookingRepository bookingRepo;
    @Mock private UserInterface userService;
    @Mock private WalletDepositRepository walletDepositRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ReferenceGeneratorService referenceGeneratorService;
    @Mock private BookingSupplierInvoiceService bookingSupplierInvoiceService;
    @Mock private BookingTimelineService bookingTimelineService;
    @Mock private ActivityBookingAuditSupport activityBookingAuditSupport;
    @Mock private NotificationHelper notificationHelper;

    private BookingService bookingService;

    private User user;
    private Booking booking;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepo);
        ReflectionTestUtils.setField(bookingService, "userService", userService);
        ReflectionTestUtils.setField(bookingService, "walletDepositRepository", walletDepositRepository);
        ReflectionTestUtils.setField(bookingService, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(bookingService, "referenceGeneratorService", referenceGeneratorService);
        ReflectionTestUtils.setField(bookingService, "bookingSupplierInvoiceService", bookingSupplierInvoiceService);
        ReflectionTestUtils.setField(bookingService, "bookingTimelineService", bookingTimelineService);
        ReflectionTestUtils.setField(bookingService, "activityBookingAuditSupport", activityBookingAuditSupport);
        ReflectionTestUtils.setField(bookingService, "notificationHelper", notificationHelper);

        user = User.builder()
                .id(USER_ID)
                .email("agent@example.com")
                .password("x")
                .fullName("Agent")
                .currency("USD")
                .balance(100.0)
                .build();

        booking = Booking.builder()
                .id(BOOKING_ID)
                .pnr("ABC123")
                .ticketNo("TCK1")
                .bookingReference("FRBOOK1")
                .bookingPrice("1200.00")
                .buyPrice("900.00")
                .originalPrice("900.00")
                .profitLoss("300.00")
                .exchangeCurrencyRate("1.0")
                .exchangeCurrency("USD")
                .providerName(Provider.SABRE)
                .status(BookingStatus.TICKETED)
                .createdBy(user)
                .build();
    }

    @Test
    void adminRefund_full_creditsWallet_reversesSupplier_andAudits_withoutDeletingPurchase() {
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(referenceGeneratorService.nextReference("FR")).thenReturn("FRREFUND1");
        when(walletDepositRepository.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminBookingRefundRequest request = AdminBookingRefundRequest.builder()
                .refundType(AdminBookingRefundRequest.RefundType.FULL)
                .supplierRefundCost(new BigDecimal("200.00"))
                .reason("Customer cancelled; supplier penalty 200")
                .build();

        AdminBookingRefundResponse response = bookingService.adminRefundBooking(BOOKING_ID, request);

        assertThat(response.getRefundedAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.getSupplierPayableReversed()).isEqualByComparingTo("700.00");
        assertThat(response.getRemainingSupplierPayable()).isEqualByComparingTo("200.00");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REFUND);

        verify(userService).addUserBalance(
                eq(USER_ID), eq(1200.0),
                eq("PARTIAL_REFUND"), eq("BookingService"),
                eq(BOOKING_ID), eq("BOOKING"), eq(USER_ID));

        ArgumentCaptor<WalletDeposit> depositCaptor = ArgumentCaptor.forClass(WalletDeposit.class);
        verify(walletDepositRepository).save(depositCaptor.capture());
        assertThat(depositCaptor.getValue().getType().name()).isEqualTo("REFUND");
        assertThat(depositCaptor.getValue().getAmount()).isEqualTo(1200.0);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertThat(txnCaptor.getValue().getType()).isEqualTo("REFUND");
        assertThat(txnCaptor.getValue().getSourceType()).isEqualTo("BOOKING");
        assertThat(txnCaptor.getValue().getSourceId()).isEqualTo(BOOKING_ID);

        verify(transactionRepository, never()).deleteAll(anyList());
        verify(walletDepositRepository, never()).deleteById(anyLong());
        verify(walletDepositRepository, never()).delete(any(WalletDeposit.class));

        verify(bookingSupplierInvoiceService).reverseSupplierPayableForRefundedBooking(
                booking, new BigDecimal("200.00"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> metaCaptor = ArgumentCaptor.forClass(Map.class);
        verify(activityBookingAuditSupport).logAdminRefund(
                eq(BOOKING_ID), eq("ABC123"), eq(BookingStatus.TICKETED),
                eq("Customer cancelled; supplier penalty 200"), metaCaptor.capture());
        assertThat(metaCaptor.getValue())
                .containsEntry("channel", "ADMIN_REFUND")
                .containsEntry("appendOnly", true)
                .containsEntry("refundType", "FULL");

        verify(bookingTimelineService).recordSystem(
                eq(BOOKING_ID), eq(BookingStatus.REFUND), eq(BookingStatus.TICKETED),
                eq("ABC123"), eq("TCK1"), eq("Customer cancelled; supplier penalty 200"));
    }

    @Test
    void adminRefund_partial_creditsNetOfDeduction() {
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(referenceGeneratorService.nextReference("FR")).thenReturn("FRREFUND2");
        when(walletDepositRepository.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminBookingRefundRequest request = AdminBookingRefundRequest.builder()
                .refundType(AdminBookingRefundRequest.RefundType.PARTIAL)
                .deductionAmount(new BigDecimal("300.00"))
                .supplierRefundCost(BigDecimal.ZERO)
                .reason("Partial refund with fee")
                .build();

        AdminBookingRefundResponse response = bookingService.adminRefundBooking(BOOKING_ID, request);

        assertThat(response.getRefundedAmount()).isEqualByComparingTo("900.00");
        assertThat(response.getDeductionAmount()).isEqualByComparingTo("300.00");
        verify(userService).addUserBalance(
                eq(USER_ID), eq(900.0),
                anyString(), anyString(), eq(BOOKING_ID), eq("BOOKING"), anyLong());
        verify(bookingSupplierInvoiceService).reverseSupplierPayableForRefundedBooking(
                booking, BigDecimal.ZERO);
    }

    @Test
    void adminRefund_alreadyRefunded_rejectedWithoutWalletOrSupplierChanges() {
        booking.setStatus(BookingStatus.REFUND);
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        AdminBookingRefundRequest request = AdminBookingRefundRequest.builder()
                .refundType(AdminBookingRefundRequest.RefundType.FULL)
                .supplierRefundCost(BigDecimal.ZERO)
                .reason("retry")
                .build();

        assertThatThrownBy(() -> bookingService.adminRefundBooking(BOOKING_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already refunded");

        verify(userService, never()).addUserBalance(anyLong(), anyDouble(), any(), any(), any(), any(), any());
        verifyNoInteractions(bookingSupplierInvoiceService);
        verifyNoInteractions(activityBookingAuditSupport);
    }

    @Test
    void adminRefund_supplierReverseFailure_propagatesAndSkipsAudit() {
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(referenceGeneratorService.nextReference("FR")).thenReturn("FRREFUND3");
        when(walletDepositRepository.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("supplier DB error"))
                .when(bookingSupplierInvoiceService)
                .reverseSupplierPayableForRefundedBooking(any(Booking.class), any(BigDecimal.class));

        AdminBookingRefundRequest request = AdminBookingRefundRequest.builder()
                .refundType(AdminBookingRefundRequest.RefundType.FULL)
                .supplierRefundCost(new BigDecimal("200.00"))
                .reason("should roll back")
                .build();

        assertThatThrownBy(() -> bookingService.adminRefundBooking(BOOKING_ID, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("supplier DB error");

        verify(activityBookingAuditSupport, never()).logAdminRefund(any(), any(), any(), any(), any());
        verify(bookingTimelineService, never()).recordSystem(any(), any(), any(), any(), any(), any());
    }
}
