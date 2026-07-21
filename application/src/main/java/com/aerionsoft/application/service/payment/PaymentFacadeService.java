package com.aerionsoft.application.service.payment;

import com.aerionsoft.application.dto.payment.CheckoutResult;
import com.aerionsoft.application.dto.payment.PaymentResult;
import com.aerionsoft.application.dto.payment.tabby.TabbyCheckoutCommand;
import com.aerionsoft.application.enums.wallet.PaymentProvider;

public interface PaymentFacadeService {

    CheckoutResult checkout(PaymentProvider provider, TabbyCheckoutCommand command, Long userId);

    PaymentResult confirm(PaymentProvider provider, String paymentId);

}
