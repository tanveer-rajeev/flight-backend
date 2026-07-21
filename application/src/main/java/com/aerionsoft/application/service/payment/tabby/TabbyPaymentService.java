package com.aerionsoft.application.service.payment.tabby;

import com.aerionsoft.application.dto.payment.tabby.TabbyCheckoutCommand;
import com.aerionsoft.application.dto.payment.tabby.TabbyCheckoutResult;
import com.aerionsoft.application.dto.payment.tabby.TabbyWebhookPayload;
import com.aerionsoft.application.entity.paymentGateway.TabbyPayment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TabbyPaymentService {

    /**
     * Creates a Tabby checkout session for a given order/cart and persists
     * a PENDING TabbyPaymentEntity before redirecting the customer.
     *
     * @return the checkout session result, including the web_url to redirect the customer to
     */
    TabbyCheckoutResult initiateCheckout(TabbyCheckoutCommand command);

    /**
     * Called from the success/cancel/failure redirect endpoints.
     * Retrieves the authoritative payment status from Tabby (never trusts
     * the redirect query params alone), and updates the persisted entity.
     *
     * @param tabbyPaymentId the Tabby payment id returned in the redirect query param
     * @return the updated payment entity reflecting Tabby's actual status
     */
    TabbyPayment confirmPayment(String tabbyPaymentId);

    /**
     * Processes an inbound Tabby webhook payload: verifies authenticity,
     * maps the event to the local entity, and updates status idempotently.
     * Safe to call multiple times for the same event.
     */
    void handleWebhook(TabbyWebhookPayload payload);

    /**
     * Looks up a locally persisted Tabby payment by our own order/reference id,
     * without calling Tabby. Used for read paths (order status pages, etc.).
     */
    Optional<TabbyPayment> findByReferenceId(String referenceId);


    /**
     * Looks up a locally persisted Tabby payment by our own payment id,
     * without calling Tabby. Used for read paths (order status pages, etc.).
     */
    Optional<TabbyPayment> findByPaymentId(String paymentId);

    TabbyPayment capturePayment(String tabbyPaymentId, BigDecimal amount);

    List<TabbyPayment> findAll();

}