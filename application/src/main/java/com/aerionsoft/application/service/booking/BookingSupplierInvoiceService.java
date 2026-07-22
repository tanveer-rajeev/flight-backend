package com.aerionsoft.application.service.booking;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.Booking.Booking;
import com.aerionsoft.application.entity.client.Invoice;
import com.aerionsoft.application.entity.client.SupplierTransactionHistory;
import com.aerionsoft.application.entity.client.SupplierTransactionHistoryDetail;
import com.aerionsoft.application.enums.client.InvoiceStatus;
import com.aerionsoft.application.repository.client.InvoiceRepository;
import com.aerionsoft.application.repository.client.SupplierRepository;
import com.aerionsoft.application.repository.client.SupplierTransactionHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Service
public class BookingSupplierInvoiceService {

    private static final String REVERSAL_MARKER = "Reversed for refunded booking";
    private static final Logger log = Logger.getLogger(BookingSupplierInvoiceService.class.getName());

    private final InvoiceRepository invoiceRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierTransactionHistoryRepository supplierTransactionHistoryRepository;

    public BookingSupplierInvoiceService(
            InvoiceRepository invoiceRepository,
            SupplierRepository supplierRepository,
            SupplierTransactionHistoryRepository supplierTransactionHistoryRepository) {
        this.invoiceRepository = invoiceRepository;
        this.supplierRepository = supplierRepository;
        this.supplierTransactionHistoryRepository = supplierTransactionHistoryRepository;
    }

    /**
     * When a booking is fully refunded, remove the entire supplier payable and record a reversal.
     */
    @Transactional
    public void reverseSupplierPayableForRefundedBooking(Booking booking) {
        reverseSupplierPayableForRefundedBooking(booking, BigDecimal.ZERO);
    }

    /**
     * Legacy ratio-based supplier reversal (customer refund ratio × buy price).
     */
    @Transactional
    public void reverseSupplierPayableForRefundedBookingByRatio(Booking booking, BigDecimal refundRatio) {
        if (booking == null || booking.getId() == null) {
            return;
        }

        BigDecimal ratio = (refundRatio == null || refundRatio.compareTo(BigDecimal.ONE) > 0)
                ? BigDecimal.ONE
                : (refundRatio.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : refundRatio);

        List<SupplierTransactionHistory> transactions = findReversibleTransactions(booking);
        if (transactions.isEmpty()) {
            log.info("No supplier invoice transaction found to reverse for refunded booking [" + booking.getId() + "]");
            return;
        }

        Set<Long> reversedInvoiceIds = new HashSet<>();
        for (SupplierTransactionHistory original : transactions) {
            reverseTransactionByRatio(booking, original, ratio);
            if (original.getInvoiceId() != null) {
                reversedInvoiceIds.add(original.getInvoiceId());
            }
        }

        markInvoicesRejected(reversedInvoiceIds);

        log.info("Reversed " + transactions.size() + " supplier transaction(s) for refunded booking ["
                + booking.getId() + "] with ratio " + ratio);
    }

    /**
     * When a booking is refunded, adjust supplier payable using an explicit supplier refund cost.
     *
     * @param supplierRefundCost amount the supplier keeps (remaining payable for this PNR).
     *                           Reversal amount = originalPayable - supplierRefundCost per transaction.
     */
    @Transactional
    public void reverseSupplierPayableForRefundedBooking(Booking booking, BigDecimal supplierRefundCost) {
        if (booking == null || booking.getId() == null) {
            return;
        }

        BigDecimal remainingSupplierCost = normalizeSupplierRefundCost(supplierRefundCost);

        List<SupplierTransactionHistory> transactions = findReversibleTransactions(booking);
        if (transactions.isEmpty()) {
            log.info("No supplier invoice transaction found to reverse for refunded booking [" + booking.getId() + "]");
            return;
        }

        Set<Long> reversedInvoiceIds = new HashSet<>();
        for (SupplierTransactionHistory original : transactions) {
            BigDecimal fullAmount = original.getPayableAmount();
            if (fullAmount == null || fullAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal keepForTxn = remainingSupplierCost.min(fullAmount);
            BigDecimal amountToReverse = fullAmount.subtract(keepForTxn);
            remainingSupplierCost = remainingSupplierCost.subtract(keepForTxn);

            reverseTransactionAmount(booking, original, amountToReverse, keepForTxn, supplierRefundCost, null);
            if (original.getInvoiceId() != null) {
                reversedInvoiceIds.add(original.getInvoiceId());
            }
        }

        markInvoicesRejected(reversedInvoiceIds);

        log.info("Reversed supplier payable for refunded booking [" + booking.getId()
                + "] with supplierRefundCost " + normalizeSupplierRefundCost(supplierRefundCost));
    }

    /**
     * Append-only buy-price adjustment for admin booking edit.
     * Increases or decreases supplier payable by (newBuyPrice - oldBuyPrice) without deleting history.
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustPayableForBuyPriceChange(Booking booking, BigDecimal oldBuyPrice, BigDecimal newBuyPrice) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        BigDecimal oldBuy = oldBuyPrice != null ? oldBuyPrice : BigDecimal.ZERO;
        BigDecimal newBuy = newBuyPrice != null ? newBuyPrice : BigDecimal.ZERO;
        BigDecimal delta = newBuy.subtract(oldBuy);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        List<SupplierTransactionHistory> originals = findReversibleTransactions(booking);
        if (originals.isEmpty()) {
            log.info("No supplier invoice transaction found to adjust buy price for booking ["
                    + booking.getId() + "]");
            return;
        }

        // Apply full delta against the first open payable row for this booking
        SupplierTransactionHistory original = originals.get(0);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            increasePayable(booking, original, delta, oldBuy, newBuy);
        } else {
            reverseTransactionAmount(booking, original, delta.abs(), newBuy, null, null);
        }
    }

    private void increasePayable(
            Booking booking,
            SupplierTransactionHistory original,
            BigDecimal amount,
            BigDecimal oldBuyPrice,
            BigDecimal newBuyPrice) {
        if (original.getSupplierId() != null) {
            supplierRepository.findById(original.getSupplierId()).ifPresent(supplier -> {
                BigDecimal current = supplier.getPayableAmount() != null ? supplier.getPayableAmount() : BigDecimal.ZERO;
                supplier.setPayableAmount(current.add(amount));
                supplierRepository.save(supplier);
            });
        }

        String pnrLabel = booking.getPnr() != null && !booking.getPnr().isBlank() ? booking.getPnr() : "—";
        SupplierTransactionHistory adjustment = SupplierTransactionHistory.builder()
                .invoiceItemId(original.getInvoiceItemId())
                .invoiceId(original.getInvoiceId())
                .agencyId(original.getAgencyId())
                .ledgerId(original.getLedgerId())
                .supplierId(original.getSupplierId())
                .payableAmount(amount)
                .title("Buy price adjustment: " + pnrLabel)
                .description("Buy price adjusted for booking id " + booking.getId()
                        + " | oldBuy " + oldBuyPrice.toPlainString()
                        + " | newBuy " + newBuyPrice.toPlainString())
                .createdDate(UserDateTimeUtil.now())
                .build();

        List<SupplierTransactionHistoryDetail> details = new ArrayList<>();
        details.add(SupplierTransactionHistoryDetail.builder().key("pnr").value(pnrLabel).build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("bookingId").value(String.valueOf(booking.getId())).build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("oldBuyPrice").value(oldBuyPrice.toPlainString()).build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("newBuyPrice").value(newBuyPrice.toPlainString()).build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("adjustmentAmount").value(amount.toPlainString()).build());
        for (SupplierTransactionHistoryDetail detail : details) {
            detail.setSupplierTransactionHistory(adjustment);
        }
        adjustment.setDetails(details);
        supplierTransactionHistoryRepository.save(adjustment);
    }

    /**
     * Record a reissue charge against the booking's existing supplier/ledger linkage.
     * Increases supplier payable and appends an audit row (append-only).
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordReissueCharge(
            Booking booking,
            BigDecimal supplierReissueCostUsd,
            LocalDate reissueDate,
            Long ticketActionRequestId) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        BigDecimal amount = supplierReissueCostUsd != null ? supplierReissueCostUsd : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Skipping supplier reissue charge for booking [" + booking.getId() + "]: zero amount");
            return;
        }

        List<SupplierTransactionHistory> originals = findReversibleTransactions(booking);
        if (originals.isEmpty()) {
            log.info("No supplier invoice transaction found for reissue on booking [" + booking.getId() + "]");
            return;
        }

        SupplierTransactionHistory original = originals.get(0);
        if (original.getSupplierId() != null) {
            supplierRepository.findById(original.getSupplierId()).ifPresent(supplier -> {
                BigDecimal current = supplier.getPayableAmount() != null ? supplier.getPayableAmount() : BigDecimal.ZERO;
                supplier.setPayableAmount(current.add(amount));
                supplierRepository.save(supplier);
            });
        }

        String pnrLabel = booking.getPnr() != null && !booking.getPnr().isBlank() ? booking.getPnr() : "—";
        String reissueDateLabel = reissueDate != null ? reissueDate.toString() : "—";
        SupplierTransactionHistory reissueTxn = SupplierTransactionHistory.builder()
                .invoiceItemId(original.getInvoiceItemId())
                .invoiceId(original.getInvoiceId())
                .agencyId(original.getAgencyId())
                .ledgerId(original.getLedgerId())
                .supplierId(original.getSupplierId())
                .payableAmount(amount)
                .title("Reissue: " + pnrLabel)
                .description("Ticket reissue for booking id " + booking.getId()
                        + " | ticketActionRequestId " + ticketActionRequestId
                        + " | reissueDate " + reissueDateLabel
                        + " | supplierReissueCost " + amount.toPlainString())
                .createdDate(UserDateTimeUtil.now())
                .build();

        List<SupplierTransactionHistoryDetail> details = new ArrayList<>();
        details.add(SupplierTransactionHistoryDetail.builder().key("pnr").value(pnrLabel).build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("bookingId").value(String.valueOf(booking.getId())).build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("ticketActionRequestId").value(String.valueOf(ticketActionRequestId)).build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("reissueDate").value(reissueDateLabel).build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("supplierReissueCost").value(amount.toPlainString()).build());
        for (SupplierTransactionHistoryDetail detail : details) {
            detail.setSupplierTransactionHistory(reissueTxn);
        }
        reissueTxn.setDetails(details);
        supplierTransactionHistoryRepository.save(reissueTxn);
    }

    private BigDecimal normalizeSupplierRefundCost(BigDecimal supplierRefundCost) {
        if (supplierRefundCost == null || supplierRefundCost.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return supplierRefundCost;
    }

    private void markInvoicesRejected(Set<Long> reversedInvoiceIds) {
        for (Long invoiceId : reversedInvoiceIds) {
            invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                invoice.setStatus(InvoiceStatus.REJECTED);
                invoiceRepository.save(invoice);
            });
        }
    }

    private List<SupplierTransactionHistory> findReversibleTransactions(Booking booking) {
        String bookingRef = " " + booking.getId() + " |";
        List<SupplierTransactionHistory> results = new ArrayList<>();

        List<Invoice> invoices = invoiceRepository.findByInvoiceDetailsContaining(bookingRef);
        for (Invoice invoice : invoices) {
            results.addAll(supplierTransactionHistoryRepository
                    .findByInvoiceIdAndDescriptionNotContaining(invoice.getId(), REVERSAL_MARKER));
        }

        if (results.isEmpty()) {
            results.addAll(supplierTransactionHistoryRepository
                    .findByDescriptionContainingAndDescriptionNotContaining(
                            " id " + booking.getId() + " |", REVERSAL_MARKER));
        }

        return results.stream()
                .filter(txn -> txn.getDescription() == null || !txn.getDescription().contains(REVERSAL_MARKER))
                .filter(txn -> txn.getPayableAmount() != null && txn.getPayableAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private void reverseTransactionByRatio(Booking booking, SupplierTransactionHistory original, BigDecimal refundRatio) {
        BigDecimal fullAmount = original.getPayableAmount();
        if (fullAmount == null || fullAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal amountToReverse = fullAmount.multiply(refundRatio)
                .setScale(4, java.math.RoundingMode.HALF_UP);
        if (amountToReverse.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal remaining = fullAmount.subtract(amountToReverse).max(BigDecimal.ZERO);
        reverseTransactionAmount(booking, original, amountToReverse, remaining, null, refundRatio);
    }

    private void reverseTransactionAmount(
            Booking booking,
            SupplierTransactionHistory original,
            BigDecimal amountToReverse,
            BigDecimal remainingSupplierPayable,
            BigDecimal supplierRefundCost,
            BigDecimal refundRatio) {
        if (amountToReverse == null || amountToReverse.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (original.getSupplierId() != null) {
            supplierRepository.findById(original.getSupplierId()).ifPresent(supplier -> {
                BigDecimal current = supplier.getPayableAmount() != null ? supplier.getPayableAmount() : BigDecimal.ZERO;
                BigDecimal updated = current.subtract(amountToReverse);
                if (updated.compareTo(BigDecimal.ZERO) < 0) {
                    updated = BigDecimal.ZERO;
                }
                supplier.setPayableAmount(updated);
                supplierRepository.save(supplier);
            });
        }

        String pnrLabel = booking.getPnr() != null && !booking.getPnr().isBlank() ? booking.getPnr() : "—";
        String descriptionSuffix = supplierRefundCost != null
                ? " | supplierRefundCost " + supplierRefundCost.toPlainString()
                : (refundRatio != null ? " | ratio " + refundRatio.toPlainString() : "");

        SupplierTransactionHistory reversal = SupplierTransactionHistory.builder()
                .invoiceItemId(original.getInvoiceItemId())
                .invoiceId(original.getInvoiceId())
                .agencyId(original.getAgencyId())
                .ledgerId(original.getLedgerId())
                .supplierId(original.getSupplierId())
                .payableAmount(amountToReverse.negate())
                .title("Refund reversal: " + pnrLabel)
                .description(REVERSAL_MARKER + " id " + booking.getId()
                        + " | Reversal of supplier txn " + original.getId()
                        + descriptionSuffix)
                .createdDate(UserDateTimeUtil.now())
                .build();

        List<SupplierTransactionHistoryDetail> details = reversalDetails(booking, original, amountToReverse, remainingSupplierPayable);
        if (supplierRefundCost != null) {
            appendReversalDetail(details, "supplierRefundCost", supplierRefundCost.toPlainString());
        } else if (refundRatio != null) {
            appendReversalDetail(details, "refundRatio", refundRatio.toPlainString());
        }
        for (SupplierTransactionHistoryDetail detail : details) {
            detail.setSupplierTransactionHistory(reversal);
        }
        reversal.setDetails(details);

        supplierTransactionHistoryRepository.save(reversal);
    }

    private List<SupplierTransactionHistoryDetail> reversalDetails(
            Booking booking,
            SupplierTransactionHistory original,
            BigDecimal amountToReverse,
            BigDecimal remainingSupplierPayable) {
        String pnrLabel = booking.getPnr() != null && !booking.getPnr().isBlank() ? booking.getPnr() : "—";
        List<SupplierTransactionHistoryDetail> details = new ArrayList<>();
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("pnr")
                .value(pnrLabel)
                .build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("bookingId")
                .value(String.valueOf(booking.getId()))
                .build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("reversedAmount")
                .value(amountToReverse.toPlainString())
                .build());
        details.add(SupplierTransactionHistoryDetail.builder()
                .key("remainingSupplierPayable")
                .value(remainingSupplierPayable != null ? remainingSupplierPayable.toPlainString() : "0")
                .build());
        return details;
    }

    private void appendReversalDetail(List<SupplierTransactionHistoryDetail> details, String key, String value) {
        SupplierTransactionHistoryDetail detail = SupplierTransactionHistoryDetail.builder()
                .key(key)
                .value(value)
                .build();
        details.add(detail);
    }
}
