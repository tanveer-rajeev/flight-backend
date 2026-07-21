package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.dto.sms.BulkSmsRequest;
import com.aerionsoft.application.dto.sms.SingleSmsRequest;
import com.aerionsoft.application.dto.sms.SmsCredentialsRequest;
import com.aerionsoft.application.dto.sms.SmsResponse;
import com.aerionsoft.application.entity.SmsCredentials;
import com.aerionsoft.application.entity.SmsLog;
import com.aerionsoft.application.repository.sms.SmsCredentialsRepository;
import com.aerionsoft.application.repository.sms.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final SmsCredentialsRepository smsCredentialsRepository;
    private final SmsLogRepository smsLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public SmsResponse sendSingleSms(SingleSmsRequest request) {
        try {
            SmsCredentials credentials = getActiveSmsCredentials();

            // Send SMS using provider API
            String response = sendSmsViaProvider(credentials, List.of(request.getPhoneNumber()), request.getMessage());

            // Log success
            SmsLog smsLog = SmsLog.builder()
                .phoneNumbers(new String[]{request.getPhoneNumber()})
                .message(request.getMessage())
                .status(SmsLog.SmsStatus.SENT)
                .providerResponse(response)
                .sentAt(UserDateTimeUtil.now())
                .build();

            smsLogRepository.save(smsLog);

            return SmsResponse.builder()
                .id(smsLog.getId())
                .phoneNumbers(List.of(request.getPhoneNumber()))
                .message(request.getMessage())
                .status("SENT")
                .build();

        } catch (Exception e) {
            log.error("Failed to send SMS", e);

            // Log failure
            SmsLog smsLog = SmsLog.builder()
                .phoneNumbers(new String[]{request.getPhoneNumber()})
                .message(request.getMessage())
                .status(SmsLog.SmsStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();

            smsLogRepository.save(smsLog);

            return SmsResponse.builder()
                .id(smsLog.getId())
                .phoneNumbers(List.of(request.getPhoneNumber()))
                .message(request.getMessage())
                .status("FAILED")
                .errorMessage(e.getMessage())
                .build();
        }
    }

    @Transactional
    public SmsResponse sendBulkSms(BulkSmsRequest request) {
        try {
            SmsCredentials credentials = getActiveSmsCredentials();

            // Send SMS using provider API
            String response = sendSmsViaProvider(credentials, request.getPhoneNumbers(), request.getMessage());

            // Log success
            SmsLog smsLog = SmsLog.builder()
                .phoneNumbers(request.getPhoneNumbers().toArray(new String[0]))
                .message(request.getMessage())
                .status(SmsLog.SmsStatus.SENT)
                .providerResponse(response)
                .sentAt(UserDateTimeUtil.now())
                .build();

            smsLogRepository.save(smsLog);

            return SmsResponse.builder()
                .id(smsLog.getId())
                .phoneNumbers(request.getPhoneNumbers())
                .message(request.getMessage())
                .status("SENT")
                .build();

        } catch (Exception e) {
            log.error("Failed to send bulk SMS", e);

            // Log failure
            SmsLog smsLog = SmsLog.builder()
                .phoneNumbers(request.getPhoneNumbers().toArray(new String[0]))
                .message(request.getMessage())
                .status(SmsLog.SmsStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();

            smsLogRepository.save(smsLog);

            return SmsResponse.builder()
                .id(smsLog.getId())
                .phoneNumbers(request.getPhoneNumbers())
                .message(request.getMessage())
                .status("FAILED")
                .errorMessage(e.getMessage())
                .build();
        }
    }

    public void saveSmsCredentials(SmsCredentialsRequest request) {
        // Deactivate existing credentials
        smsCredentialsRepository.findActiveCredentials()
            .ifPresent(existing -> {
                existing.setIsActive(false);
                smsCredentialsRepository.save(existing);
            });

        // Save new credentials
        SmsCredentials credentials = SmsCredentials.builder()
            .providerName(request.getProviderName())
            .apiKey(request.getApiKey())
            .apiSecret(request.getApiSecret())
            .senderId(request.getSenderId())
            .baseUrl(request.getBaseUrl())
            .isActive(true)
            .build();

        smsCredentialsRepository.save(credentials);
    }

    private SmsCredentials getActiveSmsCredentials() {
        return smsCredentialsRepository.findActiveCredentials()
            .orElseThrow(() -> ServiceExceptions.emailError("No active SMS credentials found"));
    }

    private String sendSmsViaProvider(SmsCredentials credentials, List<String> phoneNumbers, String message) {
        try {
            // Generic SMS sending implementation - can be customized for specific providers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + credentials.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", phoneNumbers);
            requestBody.put("message", message);
            requestBody.put("from", credentials.getSenderId());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                credentials.getBaseUrl() + "/sms/send",
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw ServiceExceptions.emailError("SMS provider returned error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to send SMS via provider", e);
            throw ServiceExceptions.emailError("Failed to send SMS: " + e.getMessage());
        }
    }
}
