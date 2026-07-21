package com.aerionsoft.application.service.payment;

import com.aerionsoft.application.entity.paymentGateway.TabbyPayment;
import com.aerionsoft.application.enums.payment.PaymentStatus;
import com.aerionsoft.application.exception.TabbyPaymentNotFoundException;
import com.aerionsoft.application.repository.payment.TabbyPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Isolates every DB write behind a short-lived transaction, kept separate
 * from any external HTTP call to Tabby. Never call tabbyApiClient from inside
 * a method here - the whole point is these transactions stay fast and don't
 * hold a DB connection open while waiting on a network response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TabbyPaymentStatusUpdater {

    private final TabbyPaymentRepository tabbyPaymentRepository;

    private static final Set<PaymentStatus> TERMINAL_STATUSES =
            Set.of(PaymentStatus.CLOSED, PaymentStatus.REJECTED, PaymentStatus.EXPIRED);

    /**
     * Re-checks terminal status inside the transaction (not just before it),
     * to close the race window between the caller's initial check and this write.
     * Returns empty if another thread already finalized this payment first.
     */
    @Transactional
    public Optional<TabbyPayment> applyStatusIfNotTerminal(String tabbyPaymentId, PaymentStatus newStatus) {
        TabbyPayment entity = tabbyPaymentRepository.findByPaymentId(tabbyPaymentId)
                .orElseThrow(() -> new TabbyPaymentNotFoundException(tabbyPaymentId));

        if (TERMINAL_STATUSES.contains(entity.getStatus())) {
            log.debug("Skipping status update for {} - already terminal at {}", tabbyPaymentId, entity.getStatus());
            return Optional.empty();
        }

        entity.setStatus(newStatus);

        try {
            tabbyPaymentRepository.save(entity);
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Another thread (webhook vs /confirm racing) already updated this row first.
            log.debug("Lost the race updating {} to {} - another path already handled it", tabbyPaymentId, newStatus);
            return Optional.empty();
        }

        return Optional.of(entity);
    }

    @Transactional
    public TabbyPayment saveNewPayment(TabbyPayment payment) {
        return tabbyPaymentRepository.save(payment);
    }
}
