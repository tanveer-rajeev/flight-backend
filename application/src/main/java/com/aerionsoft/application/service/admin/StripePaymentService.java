package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.dto.payment.PaymentConfirmationRequest;
import com.aerionsoft.application.dto.payment.PaymentIntentRequest;
import com.aerionsoft.application.dto.payment.PaymentIntentResponse;
import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.paymentGateway.Payment;
import com.aerionsoft.application.enums.booking.BookingStatus;
import com.aerionsoft.application.repository.payment.PaymentRepository;
import com.aerionsoft.application.repository.booking.BookingRepository;
import com.aerionsoft.application.service.booking.BookingTimelineService;
import com.aerionsoft.application.service.notification.NotificationHelper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class StripePaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationHelper notificationHelper;

    @Autowired
    private BookingTimelineService bookingTimelineService;

    @Value("${stripe.currency}")
    private String defaultCurrency;

    @Transactional
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) throws StripeException {
        // Validate booking exists
        Optional<Booking> bookingOpt = bookingRepository.findById(request.getBookingId());
        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException("Booking", request.getBookingId());
        }

        Booking booking = bookingOpt.get();

        // Check if payment already exists for this booking
        Optional<Payment> existingPayment = paymentRepository.findByBookingIdAndStatus(
            request.getBookingId(), "succeeded");
        if (existingPayment.isPresent()) {
            throw ServiceExceptions.notFound("Payment already completed for booking: " + request.getBookingId());
        }

        // Create metadata for Stripe
        Map<String, String> metadata = new HashMap<>();
        metadata.put("booking_id", request.getBookingId().toString());
        metadata.put("pnr", booking.getPnr() != null ? booking.getPnr() : "");
        metadata.put("flight_number", request.getFlightNumber() != null ? request.getFlightNumber() : "");

        // Create payment intent with Stripe
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency() != null ? request.getCurrency() : defaultCurrency)
                .setDescription(request.getDescription() != null ? request.getDescription() :
                    "Flight booking payment for PNR: " + booking.getPnr())
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        // Save payment record to database
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .stripePaymentIntentId(paymentIntent.getId())
                .amount(BigDecimal.valueOf(request.getAmount()).divide(BigDecimal.valueOf(100))) // Convert cents to dollars
                .currency(paymentIntent.getCurrency().toUpperCase())
                .status(paymentIntent.getStatus())
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .description(paymentIntent.getDescription())
                .stripeCreatedAt(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(paymentIntent.getCreated()),
                    ZoneId.systemDefault()))
                .build();

        paymentRepository.save(payment);

        log.info("Created payment intent {} for booking {}", paymentIntent.getId(), request.getBookingId());

        return PaymentIntentResponse.builder()
                .clientSecret(paymentIntent.getClientSecret())
                .paymentIntentId(paymentIntent.getId())
                .status(paymentIntent.getStatus())
                .amount(request.getAmount())
                .currency(paymentIntent.getCurrency().toUpperCase())
                .bookingId(request.getBookingId())
                .description(paymentIntent.getDescription())
                .build();
    }

    @Transactional
    public PaymentIntentResponse confirmPayment(PaymentConfirmationRequest request) throws StripeException {
        // Retrieve payment intent from Stripe
        PaymentIntent paymentIntent = PaymentIntent.retrieve(request.getPaymentIntentId());

        // Update local payment record
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(request.getPaymentIntentId());
        if (paymentOpt.isEmpty()) {
            throw ServiceExceptions.notFound("Payment record not found for intent: " + request.getPaymentIntentId());
        }

        Payment payment = paymentOpt.get();
        payment.setStatus(paymentIntent.getStatus());
        payment.setPaymentMethodId(request.getPaymentMethodId());
        paymentRepository.save(payment);

        // If payment succeeded, update booking status
        if ("succeeded".equals(paymentIntent.getStatus())) {
            updateBookingStatusAfterPayment(payment.getBookingId());
        }

        log.info("Payment {} status updated to: {}", paymentIntent.getId(), paymentIntent.getStatus());

        return PaymentIntentResponse.builder()
                .clientSecret(paymentIntent.getClientSecret())
                .paymentIntentId(paymentIntent.getId())
                .status(paymentIntent.getStatus())
                .amount(paymentIntent.getAmount())
                .currency(paymentIntent.getCurrency().toUpperCase())
                .bookingId(payment.getBookingId())
                .description(paymentIntent.getDescription())
                .build();
    }

    @Transactional
    public void handleWebhookEvent(PaymentIntent paymentIntent) {
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());
        if (paymentOpt.isEmpty()) {
            log.warn("Payment record not found for webhook event: {}", paymentIntent.getId());
            return;
        }

        Payment payment = paymentOpt.get();
        String oldStatus = payment.getStatus();
        payment.setStatus(paymentIntent.getStatus());
        paymentRepository.save(payment);

        log.info("Webhook: Payment {} status changed from {} to {}",
            paymentIntent.getId(), oldStatus, paymentIntent.getStatus());

        // If payment succeeded, update booking
        if ("succeeded".equals(paymentIntent.getStatus()) && !"succeeded".equals(oldStatus)) {
            updateBookingStatusAfterPayment(payment.getBookingId());
        }
    }

    private void updateBookingStatusAfterPayment(Long bookingId) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            BookingStatus oldStatus = booking.getStatus();
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setUpdatedAt(UserDateTimeUtil.now());
            bookingRepository.save(booking);
            log.info("Updated booking {} status to CONFIRMED after successful payment", bookingId);

            // Record timeline event
            bookingTimelineService.recordSystem(bookingId, BookingStatus.CONFIRMED, oldStatus,
                    booking.getPnr(), booking.getTicketNo(), "Payment confirmed via Stripe");

            // Send email notification to user
            try {
                notificationHelper.sendBookingConfirmation(
                    booking.getCreatedBy().getId(),
                    booking.getCreatedBy().getEmail(),
                    booking.getBookingReference(),
                    booking.getBookingPrice(),
                    booking.getExchangeCurrency() != null ? booking.getExchangeCurrency() : "USD",
                    booking.getId(),
                    true  // Send email
                );
                log.info("Sent booking confirmation email for booking {}", bookingId);
            } catch (Exception e) {
                log.error("Failed to send booking confirmation email for booking {}: {}", bookingId, e.getMessage());
            }
        }
    }

    public PaymentIntentResponse getPaymentStatus(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
        if (paymentOpt.isEmpty()) {
            throw ServiceExceptions.notFound("Payment record not found for intent: " + paymentIntentId);
        }

        Payment payment = paymentOpt.get();

        return PaymentIntentResponse.builder()
                .clientSecret(paymentIntent.getClientSecret())
                .paymentIntentId(paymentIntent.getId())
                .status(paymentIntent.getStatus())
                .amount(paymentIntent.getAmount())
                .currency(paymentIntent.getCurrency().toUpperCase())
                .bookingId(payment.getBookingId())
                .description(paymentIntent.getDescription())
                .build();
    }


    // now add money feature

}
