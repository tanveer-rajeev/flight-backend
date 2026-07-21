package com.aerionsoft.application.service.payment.tabby;

import com.aerionsoft.application.dto.payment.tabby.*;

public interface TabbyApiClient {
    TabbyCheckoutSessionResponse createCheckoutSession(TabbyCheckoutSessionRequest request);
    TabbyPaymentDetailsResponse getPayment(String paymentId);
    TabbyPaymentDetailsResponse capturePayment(String paymentId, CapturePaymentRequest request);
    void refundPayment(String paymentId, RefundPaymentRequest request);
    void closePayment(String paymentId);
}
