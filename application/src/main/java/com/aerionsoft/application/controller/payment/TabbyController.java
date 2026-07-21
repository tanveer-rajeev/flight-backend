package com.aerionsoft.application.controller.payment;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.entity.paymentGateway.TabbyPayment;
import com.aerionsoft.application.service.payment.tabby.TabbyPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments/tabby")
@RequiredArgsConstructor
public class TabbyController extends BaseController {

    private final TabbyPaymentService tabbyPaymentService;

    @GetMapping("/{id}")
    public ResponseEntity<TabbyPayment> getByPaymentId(@PathVariable String id) {
        return ResponseEntity.ok(tabbyPaymentService.findByPaymentId(id).get());
    }

    @GetMapping
    public ResponseEntity<List<TabbyPayment>> getAll() {
        return ResponseEntity.ok(tabbyPaymentService.findAll());
    }

    /**
     * Manual capture, only relevant if your Tabby account uses manual (not auto) capture.
     */
    @PostMapping("/{tabbyPaymentId}/capture")
    public ResponseEntity<TabbyPayment> capturePayment(
            @PathVariable String tabbyPaymentId,
            @RequestParam(required = false) java.math.BigDecimal amount) {
        return ResponseEntity.ok(tabbyPaymentService.capturePayment(tabbyPaymentId, amount));
    }
}
