package com.aerionsoft.application.service.errorlog;

import com.aerionsoft.application.enums.common.MicroserviceType;
import com.aerionsoft.application.exception.MicroserviceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MicroserviceErrorHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handle error responses from microservices and throw appropriate exceptions
     */
    public void handleErrorResponse(MicroserviceType serviceType, ResponseEntity<?> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            String errorCode = extractErrorCode(response);
            String errorMessage = extractErrorMessage(response);
            throw new MicroserviceException(serviceType, errorCode, errorMessage, response.getBody());
        }
    }

    /**
     * Handle error responses with custom error code
     */
    public void handleErrorResponse(MicroserviceType serviceType, String errorCode, String message) {
        throw new MicroserviceException(serviceType, errorCode, message);
    }

    /**
     * Handle generic service failures
     */
    public void handleServiceFailure(MicroserviceType serviceType, String message, Object responseData) {
        throw new MicroserviceException(serviceType, message, responseData);
    }

    /**
     * Parse and handle JSON error responses
     */
    public void handleJsonErrorResponse(MicroserviceType serviceType, String jsonResponse) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            String errorCode = jsonNode.has("errorCode") ? jsonNode.get("errorCode").asText() : "UNKNOWN";
            String message = jsonNode.has("message") ? jsonNode.get("message").asText() :
                           jsonNode.has("error") ? jsonNode.get("error").asText() : "Service error occurred";

            throw new MicroserviceException(serviceType, errorCode, message, jsonNode);
        } catch (Exception e) {
            throw new MicroserviceException(serviceType, "PARSE_ERROR",
                "Failed to parse error response: " + jsonResponse);
        }
    }

    private String extractErrorCode(ResponseEntity<?> response) {
        try {
            Object body = response.getBody();
            if (body != null) {
                String bodyStr = body.toString();
                // Try to extract error code from common patterns
                if (bodyStr.contains("\"errorCode\"")) {
                    JsonNode jsonNode = objectMapper.readTree(bodyStr);
                    return jsonNode.has("errorCode") ? jsonNode.get("errorCode").asText() : "UNKNOWN";
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return response.getStatusCode().toString();
    }

    private String extractErrorMessage(ResponseEntity<?> response) {
        try {
            Object body = response.getBody();
            if (body != null) {
                String bodyStr = body.toString();
                // Try to extract message from common patterns
                if (bodyStr.contains("\"message\"")) {
                    JsonNode jsonNode = objectMapper.readTree(bodyStr);
                    return jsonNode.has("message") ? jsonNode.get("message").asText() : bodyStr;
                }
                return bodyStr;
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "Service returned " + response.getStatusCode();
    }
}
