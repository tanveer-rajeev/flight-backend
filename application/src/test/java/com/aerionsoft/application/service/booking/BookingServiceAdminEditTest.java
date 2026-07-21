package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.dto.booking.AdminBookingEditRequest;
import com.aerionsoft.application.dto.booking.AdminBookingEditResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.Booking.Traveller;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.wallet.Transaction;
import com.aerionsoft.application.entity.wallet.WalletDeposit;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.enums.booking.Provider;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.interafces.UserInterface;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.repository.booking.TravellerRepository;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.wallet.TransactionRepository;
import com.aerionsoft.application.repository.wallet.WalletDepositRepository;
import com.aerionsoft.application.service.audit.ActivityBookingAuditSupport;
import com.aerionsoft.application.service.common.CurrencyService;
import com.aerionsoft.application.service.wallet.ReferenceGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Spec / acceptance tests for {@code PATCH /api/bookings/{id}/admin/edit}.
 *
 * <p><b>Defaults under review — approve before implementing:</b>
 * <ul>
 *   <li>Append-only wallet deltas on sell-price change (never delete PURCHASE)</li>
 *   <li>Append-only supplier payable adjust on buy-price change when invoice exists</li>
 *   <li>Agency transfer: ownership move + credit old / charge new for charged amount</li>
 *   <li>Pax name edits update shared {@code Traveller} rows</li>
 *   <li>Block edits when status is REFUND or CANCELLED</li>
 *   <li>Fail-closed single DB transaction + field-level audit</li>
 * </ul>
 *
 * <p>Remove or keep these assertions in sync with {@link BookingService#adminEditBooking}.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceAdminEditTest {

    private static final Long BOOKING_ID = 42L;
    private static final Long OLD_AGENCY_ID = 10L;
    private static final Long NEW_AGENCY_ID = 20L;
    private static final Long TRAVELLER_ID = 55L;

    @Mock private BookingRepository bookingRepo;
    @Mock private UserRepository userRepo;
    @Mock private UserInterface userService;
    @Mock private TravellerRepository travellerRepository;
    @Mock private WalletDepositRepository walletDepositRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ReferenceGeneratorService referenceGeneratorService;
    @Mock private CurrencyService currencyService;
    @Mock private BookingSupplierInvoiceService bookingSupplierInvoiceService;
    @Mock private BookingTimelineService bookingTimelineService;
    @Mock private ActivityBookingAuditSupport activityBookingAuditSupport;

    private BookingService bookingService;

    private User oldAgency;
    private User newAgency;
    private Booking booking;
    private Traveller traveller;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepo);
        ReflectionTestUtils.setField(bookingService, "userService", userService);
        ReflectionTestUtils.setField(bookingService, "userRepo", userRepo);
        ReflectionTestUtils.setField(bookingService, "travellerRepository", travellerRepository);
        ReflectionTestUtils.setField(bookingService, "walletDepositRepository", walletDepositRepository);
        ReflectionTestUtils.setField(bookingService, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(bookingService, "referenceGeneratorService", referenceGeneratorService);
        ReflectionTestUtils.setField(bookingService, "currencyService", currencyService);
        ReflectionTestUtils.setField(bookingService, "bookingSupplierInvoiceService", bookingSupplierInvoiceService);
        ReflectionTestUtils.setField(bookingService, "bookingTimelineService", bookingTimelineService);
        ReflectionTestUtils.setField(bookingService, "activityBookingAuditSupport", activityBookingAuditSupport);

        oldAgency = User.builder()
                .id(OLD_AGENCY_ID)
                .email("old@agency.com")
                .password("x")
                .fullName("Old Agency")
                .currency("USD")
                .balance(5000.0)
                .build();

        newAgency = User.builder()
                .id(NEW_AGENCY_ID)
                .email("new@agency.com")
                .password("x")
                .fullName("New Agency")
                .currency("USD")
                .balance(1000.0)
                .build();

        traveller = Traveller.builder()
                .id(TRAVELLER_ID)
                .title("MR")
                .firstName("John")
                .lastName("Doe")
                .mobile("1700000000")
                .mobileCountryCode("+880")
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
                .createdBy(oldAgency)
                .createdByName("Old Agency")
                .travellerIds(String.valueOf(TRAVELLER_ID))
                .build();
    }

    private void stubLoadedBooking() {
        when(bookingRepo.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        lenient().when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(currencyService.getExchangeRateBasedOnUsd(anyString(), anyString(), any()))
                .thenReturn(1.0);
    }

    @Nested
    @DisplayName("Guards")
    class Guards {

        @Test
        void rejectsWhenAlreadyRefunded() {
            booking.setStatus(BookingStatus.REFUND);
            stubLoadedBooking();

            assertThatThrownBy(() -> bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder().reason("fix PNR").pnr("NEWPNR").build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("REFUND");

            verify(userService, never()).addUserBalance(anyLong(), anyDouble(), any(), any(), any(), any(), any());
            verify(userService, never()).deductUserBalance(
                    anyLong(), anyDouble(), anyString(), anyBoolean(), any(), any(), any(), any());
        }

        @Test
        void rejectsWhenCancelled() {
            booking.setStatus(BookingStatus.CANCELLED);
            stubLoadedBooking();

            assertThatThrownBy(() -> bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder().reason("x").pnr("NEWPNR").build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        void rejectsWhenNoEditableFieldProvided() {
            stubLoadedBooking();

            assertThatThrownBy(() -> bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder().reason("nothing").build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("at least one");
        }
    }

    @Nested
    @DisplayName("Field edits (no finance)")
    class FieldEdits {

        @Test
        void updatesPnrAndAudits() {
            stubLoadedBooking();

            AdminBookingEditResponse response = bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder().reason("reissue").pnr("XYZ789").build());

            assertThat(booking.getPnr()).isEqualTo("XYZ789");
            assertThat(response.getPnr()).isEqualTo("XYZ789");
            assertThat(response.getChanges()).containsKey("pnr");

            verify(activityBookingAuditSupport).logAdminEdit(
                    eq(BOOKING_ID), eq("XYZ789"), eq("reissue"), anyMap());
            verify(transactionRepository, never()).deleteAll(anyList());
            verify(walletDepositRepository, never()).deleteById(anyLong());
        }

        @Test
        void updatesPaxNameOnSharedTraveller() {
            stubLoadedBooking();
            when(travellerRepository.findById(TRAVELLER_ID)).thenReturn(Optional.of(traveller));
            when(travellerRepository.save(any(Traveller.class))).thenAnswer(inv -> inv.getArgument(0));

            AdminBookingEditResponse response = bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder()
                            .reason("name correction")
                            .travellers(List.of(AdminBookingEditRequest.TravellerNameUpdate.builder()
                                    .travellerId(TRAVELLER_ID)
                                    .firstName("Jon")
                                    .lastName("Doh")
                                    .build()))
                            .build());

            assertThat(traveller.getFirstName()).isEqualTo("Jon");
            assertThat(traveller.getLastName()).isEqualTo("Doh");
            assertThat(response.getTravellerChanges()).isNotEmpty();
            verify(userService, never()).addUserBalance(anyLong(), anyDouble(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Price deltas (append-only)")
    class PriceDeltas {

        @Test
        void sellPriceIncrease_chargesWalletDelta_keepsOriginalPurchase() {
            stubLoadedBooking();
            when(referenceGeneratorService.nextReference("FR")).thenReturn("FRADJ1");
            when(walletDepositRepository.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            AdminBookingEditResponse response = bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder()
                            .reason("sell price up")
                            .bookingPrice(new BigDecimal("1300.00"))
                            .build());

            assertThat(booking.getBookingPrice()).isEqualTo("1300.00");
            assertThat(booking.getProfitLoss()).isEqualTo("400.00");
            assertThat(response.getWalletDeltaCharged()).isEqualByComparingTo("100.00");

            verify(userService).deductUserBalance(
                    eq(OLD_AGENCY_ID), eq(100.0), anyString(), anyBoolean(),
                    any(), eq(BOOKING_ID), eq("BOOKING"), any());
            verify(transactionRepository, never()).deleteAll(anyList());
            verify(walletDepositRepository, never()).deleteById(anyLong());
        }

        @Test
        void sellPriceDecrease_creditsWalletDelta_appendOnly() {
            stubLoadedBooking();
            when(referenceGeneratorService.nextReference("FR")).thenReturn("FRADJ2");
            when(walletDepositRepository.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            AdminBookingEditResponse response = bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder()
                            .reason("sell price down")
                            .bookingPrice(new BigDecimal("1100.00"))
                            .build());

            assertThat(response.getWalletDeltaCredited()).isEqualByComparingTo("100.00");
            verify(userService).addUserBalance(
                    eq(OLD_AGENCY_ID), eq(100.0),
                    anyString(), eq("BookingService"), eq(BOOKING_ID), eq("BOOKING"), anyLong());
        }

        @Test
        void buyPriceChange_recomputesProfitLoss_andAdjustsSupplier() {
            stubLoadedBooking();

            AdminBookingEditResponse response = bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder()
                            .reason("buy price correction")
                            .buyPrice(new BigDecimal("850.00"))
                            .build());

            assertThat(booking.getBuyPrice()).isEqualTo("850.00");
            assertThat(booking.getProfitLoss()).isEqualTo("350.00");
            assertThat(response.getProfitLoss()).isEqualByComparingTo("350.00");

            verify(bookingSupplierInvoiceService).adjustPayableForBuyPriceChange(
                    eq(booking), eq(new BigDecimal("900.00")), eq(new BigDecimal("850.00")));
        }
    }

    @Nested
    @DisplayName("Agency transfer (append-only)")
    class AgencyTransfer {

        @Test
        void transfer_movesOwnership_creditsOldAgency_chargesNewAgency() {
            stubLoadedBooking();
            when(userRepo.findById(NEW_AGENCY_ID)).thenReturn(Optional.of(newAgency));
            when(referenceGeneratorService.nextReference("FR")).thenReturn("FRXFER1", "FRXFER2");
            when(walletDepositRepository.save(any(WalletDeposit.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.findBySourceTypeAndSourceId("BOOKING", BOOKING_ID))
                    .thenReturn(List.of(Transaction.builder()
                            .type("PURCHASE")
                            .amount(1200.0)
                            .userId(OLD_AGENCY_ID)
                            .sourceType("BOOKING")
                            .sourceId(BOOKING_ID)
                            .build()));

            AdminBookingEditResponse response = bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder()
                            .reason("move to new agency")
                            .targetUserId(NEW_AGENCY_ID)
                            .build());

            assertThat(booking.getCreatedBy().getId()).isEqualTo(NEW_AGENCY_ID);
            assertThat(booking.getCreatedByName()).isEqualTo("New Agency");
            assertThat(booking.getActingUserId()).isNull();
            assertThat(response.getTransferredFromUserId()).isEqualTo(OLD_AGENCY_ID);
            assertThat(response.getTransferredToUserId()).isEqualTo(NEW_AGENCY_ID);

            verify(userService).addUserBalance(
                    eq(OLD_AGENCY_ID), eq(1200.0),
                    anyString(), anyString(), eq(BOOKING_ID), eq("BOOKING"), any());
            verify(userService).deductUserBalance(
                    eq(NEW_AGENCY_ID), eq(1200.0), anyString(), anyBoolean(),
                    any(), eq(BOOKING_ID), eq("BOOKING"), any());

            verify(transactionRepository, never()).deleteAll(anyList());
            verify(walletDepositRepository, never()).deleteById(anyLong());
        }

        @Test
        void transfer_rejectsChildTargetUser_mustBeMotherAgency() {
            stubLoadedBooking();
            User child = User.builder()
                    .id(99L)
                    .email("child@agency.com")
                    .password("x")
                    .fullName("Child")
                    .parentUser(newAgency)
                    .currency("USD")
                    .build();
            when(userRepo.findById(99L)).thenReturn(Optional.of(child));

            assertThatThrownBy(() -> bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder()
                            .reason("bad target")
                            .targetUserId(99L)
                            .build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mother");
        }
    }

    @Nested
    @DisplayName("Transaction integrity")
    class Integrity {

        @Test
        void supplierAdjustFailure_propagates_andSkipsAudit() {
            stubLoadedBooking();
            doThrow(new RuntimeException("supplier adjust failed"))
                    .when(bookingSupplierInvoiceService)
                    .adjustPayableForBuyPriceChange(any(), any(), any());

            assertThatThrownBy(() -> bookingService.adminEditBooking(BOOKING_ID,
                    AdminBookingEditRequest.builder()
                            .reason("buy change")
                            .buyPrice(new BigDecimal("800.00"))
                            .build()))
                    .hasMessageContaining("supplier adjust failed");

            verify(activityBookingAuditSupport, never()).logAdminEdit(any(), any(), any(), any());
        }
    }
}
