package com.aerionsoft.application.controller.common;

import com.aerionsoft.application.dto.BaseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Validated
public class HealthController {

    @Value("${DB_HOST:unknown}")
    private String dbUrl;

    @Value("${DB_USER:unknown}")
    private String dbUser;

    @GetMapping("/health")
    public ResponseEntity<BaseResponse<Map<String, String>>> health() {
        Map<String, String> data = Map.of(
                "status", "ok",
                "dbUrl", dbUrl,
                "dbUser", dbUser
        );
        return ResponseEntity.ok(BaseResponse.ok(data));
    }
}
