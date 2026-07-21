package com.aerionsoft.application.service.payment;

import com.aerionsoft.application.dto.payment.PaymentCompletionRequest;

public interface PaymentCompletionService {

    void completeDeposit(PaymentCompletionRequest request);

}
