package com.aerionsoft.application.service.gateway;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.gateway.contracts.GatewayClientInterface;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebhookService {

    private final Map<String, GatewayClientInterface> adapters;

    public WebhookService(Map<String, GatewayClientInterface> adapters) {
        this.adapters = adapters;
    }

    /**
     * Get NGENIUS order status
     *
     * @param orderReference orderReference
     * @return Object
     */
    public Object getNgeniusOrderStatus(String orderReference) {
        GatewayClientInterface adapter = adapters.get("NGenius");

        if (adapter == null) {
            throw ServiceExceptions.payment("Gateway not supported: " + "NGenius");
        }

        return adapter.getOrderStatus(orderReference);
    }
}
