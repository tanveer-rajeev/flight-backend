package com.aerionsoft.application.controller.gateway;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.service.gateway.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/webhooks")
public class WebhookController extends BaseController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Ngenius call back
     *
     * @param orderReference orderReference
     * @return payment status wrapped in BaseResponse
     */
    @GetMapping("/n-genius/callback/{orderReference}")
    public ResponseEntity<BaseResponse<Object>> getNgeniusOrderStatus(@PathVariable String orderReference) {
        Object result = webhookService.getNgeniusOrderStatus(orderReference);
        return ResponseEntity.ok(BaseResponse.ok(result));
    }
}
