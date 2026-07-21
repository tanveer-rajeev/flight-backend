package com.aerionsoft.application.controller.payment;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.payment.CheckoutResult;
import com.aerionsoft.application.dto.payment.PaymentResult;
import com.aerionsoft.application.dto.payment.tabby.TabbyCheckoutCommand;
import com.aerionsoft.application.enums.wallet.PaymentProvider;
import com.aerionsoft.application.service.payment.PaymentFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class TestPaymentController extends BaseController {
    private final PaymentFacadeService paymentFacadeService;

    /**
     * Called by your own frontend to start a payment checkout.
     * Returns the redirect URL; the frontend navigates the customer there.
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResult> checkout(@RequestBody TabbyCheckoutCommand command,
                                                   @RequestParam PaymentProvider provider) {
        CheckoutResult result = paymentFacadeService.checkout(provider, command, getUserIdFromAuthentication());
        return ResponseEntity.ok(result);
    }

    /**
     * Redirects the customer's browser here after they complete
     * (or abandon) the hosted payment flow. Never trust the query param alone -
     * the service re-verifies against Tabby's Retrieve Payment API.
     */
    @GetMapping("/confirm")
    public ResponseEntity<PaymentResult> confirmPayment(@RequestParam PaymentProvider provider,
                                                       @RequestParam("payment_id") String paymentId) {
        PaymentResult result = paymentFacadeService.confirm(provider, paymentId);
        return ResponseEntity.ok(result);
    }
}
