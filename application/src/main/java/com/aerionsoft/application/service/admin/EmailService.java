package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.dto.email.*;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.entity.email.EmailCredentials;
import com.aerionsoft.application.entity.email.EmailLog;
import com.aerionsoft.application.entity.email.EmailTemplate;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.email.EmailCredentialsRepository;
import com.aerionsoft.application.repository.email.EmailLogRepository;
import com.aerionsoft.application.repository.email.EmailTemplateRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service("adminEmailService")
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailCredentialsRepository emailCredentialsRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailLogRepository emailLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public EmailResponse sendSingleEmail(SingleEmailRequest request) {
        try {
            Long businessId = request.getBusinessId() != null ? request.getBusinessId() : 0L;
            EmailCredentials credentials = getActiveEmailCredentials(businessId);
            JavaMailSender mailSender = createMailSender(credentials);

            String finalSubject = request.getSubject();
            String finalBody = request.getBody();

            // If template is specified, use template and replace variables
            if (request.getTemplateName() != null) {
                EmailTemplate template = emailTemplateRepository
                    .findByTemplateNameAndIsActiveTrueAndBusinessId(request.getTemplateName(), businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Email template", request.getTemplateName()));

                finalSubject = replaceVariables(template.getSubject(), request.getTemplateVariables());
                finalBody = replaceVariables(template.getBody(), request.getTemplateVariables());
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(credentials.getFromEmail(), credentials.getFromName());
            helper.setTo(request.getToEmail());
            helper.setSubject(finalSubject);
            helper.setText(finalBody, true);

            // Add CC emails
            if (request.getCcEmails() != null && !request.getCcEmails().isEmpty()) {
                helper.setCc(request.getCcEmails().toArray(new String[0]));
            }

            // Add BCC emails
            if (request.getBccEmails() != null && !request.getBccEmails().isEmpty()) {
                helper.setBcc(request.getBccEmails().toArray(new String[0]));
            }

            // Add attachments
            if (request.getAttachmentUrls() != null) {
                for (String attachmentUrl : request.getAttachmentUrls()) {
                    try {
                        File attachment = new File(attachmentUrl);
                        if (attachment.exists()) {
                            helper.addAttachment(attachment.getName(), attachment);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to attach file: " + attachmentUrl, e);
                    }
                }
            }

            mailSender.send(message);

            // Log success
            EmailLog emailLog = EmailLog.builder()
                .toEmail(request.getToEmail())
                .ccEmails(request.getCcEmails() != null ? request.getCcEmails().toArray(new String[0]) : null)
                .bccEmails(request.getBccEmails() != null ? request.getBccEmails().toArray(new String[0]) : null)
                .subject(finalSubject)
                .body(finalBody)
                .attachmentUrls(request.getAttachmentUrls() != null ? request.getAttachmentUrls().toArray(new String[0]) : null)
                .status(EmailLog.EmailStatus.SENT)
                .sentAt(UserDateTimeUtil.now())
                .businessId(businessId)
                .build();

            emailLogRepository.save(emailLog);

            return EmailResponse.builder()
                .id(emailLog.getId())
                .toEmail(request.getToEmail())
                .subject(finalSubject)
                .status("SENT")
                .build();

        } catch (Exception e) {
            log.error("Failed to send email", e);

            // Get user-friendly error message
            String userFriendlyMessage = getUserFriendlyEmailError(e.getMessage());

            // Log failure
            EmailLog emailLog = EmailLog.builder()
                .toEmail(request.getToEmail())
                .subject(request.getSubject())
                .body(request.getBody())
                .status(EmailLog.EmailStatus.FAILED)
                .errorMessage(e.getMessage())
                .businessId(request.getBusinessId() != null ? request.getBusinessId() : 0L)
                .build();

            emailLogRepository.save(emailLog);

            return EmailResponse.builder()
                .id(emailLog.getId())
                .toEmail(request.getToEmail())
                .subject(request.getSubject())
                .status("FAILED")
                .errorMessage(userFriendlyMessage)
                .build();
        }
    }

    @Transactional
    public List<EmailResponse> sendBulkEmail(BulkEmailRequest request) {
        List<EmailResponse> responses = new ArrayList<>();

        for (String email : request.getToEmails()) {
            SingleEmailRequest singleRequest = SingleEmailRequest.builder()
                .toEmail(email)
                .ccEmails(request.getCcEmails())
                .bccEmails(request.getBccEmails())
                .subject(request.getSubject())
                .body(request.getBody())
                .templateName(request.getTemplateName())
                .templateVariables(request.getTemplateVariables())
                .attachmentUrls(request.getAttachmentUrls())
                .businessId(request.getBusinessId())
                .build();

            responses.add(sendSingleEmail(singleRequest));
        }

        return responses;
    }

    @Transactional
    public BroadcastEmailResponse sendBroadcastEmail(BroadcastEmailRequest request) {
        List<String> recipients = resolveRecipients(request);

        if (recipients.isEmpty()) {
            return BroadcastEmailResponse.builder()
                    .totalRecipients(0)
                    .successCount(0)
                    .failureCount(0)
                    .failedEmails(Collections.emptyList())
                    .message("No recipients found.")
                    .build();
        }

        List<String> failedEmails = new ArrayList<>();
        int successCount = 0;

        for (String email : recipients) {
            SingleEmailRequest singleRequest = SingleEmailRequest.builder()
                    .toEmail(email)
                    .subject(request.getSubject())
                    .body(request.getBody())
                    .templateName(request.getTemplateName())
                    .templateVariables(request.getTemplateVariables())
                    .businessId(request.getBusinessId())
                    .build();

            EmailResponse response = sendSingleEmail(singleRequest);
            if ("SENT".equals(response.getStatus())) {
                successCount++;
            } else {
                failedEmails.add(email);
            }
        }

        return BroadcastEmailResponse.builder()
                .totalRecipients(recipients.size())
                .successCount(successCount)
                .failureCount(failedEmails.size())
                .failedEmails(failedEmails)
                .message(String.format("Broadcast complete: %d sent, %d failed out of %d recipients.",
                        successCount, failedEmails.size(), recipients.size()))
                .build();
    }

    private List<String> resolveRecipients(BroadcastEmailRequest request) {
        // Explicit email list takes highest priority
        if (request.getToEmails() != null && !request.getToEmails().isEmpty()) {
            return request.getToEmails();
        }

        // Send to specific user IDs
        if (request.getToUserIds() != null && !request.getToUserIds().isEmpty()) {
            return userRepository.findAllById(request.getToUserIds()).stream()
                    .filter(u -> u.getEmail() != null && Boolean.TRUE.equals(u.getIsActive()))
                    .map(User::getEmail)
                    .distinct()
                    .collect(Collectors.toList());
        }

        // Send to all (optionally filtered by userType)
        if (Boolean.TRUE.equals(request.getSendToAll())) {
            List<User> users;
            if (request.getUserType() != null && !request.getUserType().isBlank()) {
                users = userRepository.findByRole(request.getUserType());
            } else {
                users = userRepository.findAll();
            }
            return users.stream()
                    .filter(u -> u.getEmail() != null && Boolean.TRUE.equals(u.getIsActive()))
                    .map(User::getEmail)
                    .distinct()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public EmailTemplateResponse getTemplateByName(String templateName, Long businessId) {
        businessId = businessId != null ? businessId : 0L;
        EmailTemplate template = emailTemplateRepository
                .findByTemplateNameAndIsActiveTrueAndBusinessId(templateName, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", templateName));
        return mapToTemplateResponse(template);
    }

    public EmailTemplateResponse getTemplateById(Long id, Long businessId) {
        businessId = businessId != null ? businessId : 0L;
        EmailTemplate template = emailTemplateRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Template"));
        return mapToTemplateResponse(template);
    }

    public EmailCredentials getActiveCredentials(Long businessId) {
        businessId = businessId != null ? businessId : 0L;
        return emailCredentialsRepository.findActiveCredentialsByBusinessId(businessId)
                .orElse(null);
    }

    public EmailCredentialsRequest getActiveCredentialsDto(Long businessId) {
        EmailCredentials creds = getActiveCredentials(businessId);
        if (creds == null) return null;
        return EmailCredentialsRequest.builder()
                .smtpHost(creds.getSmtpHost())
                .smtpPort(creds.getSmtpPort())
                .username(creds.getUsername())
                .fromEmail(creds.getFromEmail())
                .fromName(creds.getFromName())
                .isSslEnabled(creds.getIsSslEnabled())
                .businessId(creds.getBusinessId())
                .build();
    }

    public EmailTemplateResponse createTemplate(EmailTemplateRequest request) {
        Long businessId = request.getBusinessId() != null ? request.getBusinessId() : 0L;

        EmailTemplate template = EmailTemplate.builder()
            .templateName(request.getTemplateName())
            .subject(request.getSubject())
            .body(request.getBody())
            .templateType(request.getTemplateType())
            .variables(request.getVariables() != null ? request.getVariables() : new ArrayList<>())
            .isActive(true)
            .businessId(businessId)
            .build();

        EmailTemplate saved = emailTemplateRepository.save(template);

        return mapToTemplateResponse(saved);
    }

    public EmailTemplateResponse updateTemplate(Long id, EmailTemplateRequest request) {
        Long businessId = request.getBusinessId() != null ? request.getBusinessId() : 0L;

        EmailTemplate template = emailTemplateRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Email template"));

        template.setTemplateName(request.getTemplateName());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setTemplateType(request.getTemplateType());
        template.setVariables(request.getVariables() != null ? request.getVariables() : new ArrayList<>());

        EmailTemplate saved = emailTemplateRepository.save(template);
        return mapToTemplateResponse(saved);
    }

    public void deleteTemplate(Long id, Long businessId) {
        businessId = businessId != null ? businessId : 0L;

        EmailTemplate template = emailTemplateRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Email template"));
        template.setIsActive(false);
        emailTemplateRepository.save(template);
    }

    public List<EmailTemplateResponse> getAllTemplates(Long businessId) {
        businessId = businessId != null ? businessId : 0L;

        return emailTemplateRepository.findByIsActiveTrueAndBusinessIdOrderByCreatedAtDesc(businessId)
            .stream()
            .map(this::mapToTemplateResponse)
            .collect(Collectors.toList());
    }

    public void saveEmailCredentials(EmailCredentialsRequest request) {
        Long businessId = request.getBusinessId() != null ? request.getBusinessId() : 0L;

        // Deactivate existing credentials for this business
        emailCredentialsRepository.findActiveCredentialsByBusinessId(businessId)
            .ifPresent(existing -> {
                existing.setIsActive(false);
                emailCredentialsRepository.save(existing);
            });

        // Save new credentials
        EmailCredentials credentials = EmailCredentials.builder()
            .smtpHost(request.getSmtpHost())
            .smtpPort(request.getSmtpPort())
            .username(request.getUsername())
            .password(request.getPassword())
            .fromEmail(request.getFromEmail())
            .fromName(request.getFromName())
            .isSslEnabled(request.getIsSslEnabled())
            .isActive(true)
            .businessId(businessId)
            .build();

        emailCredentialsRepository.save(credentials);
    }

    private EmailCredentials getActiveEmailCredentials(Long businessId) {
        return emailCredentialsRepository.findActiveCredentialsByBusinessId(businessId)
            .orElseThrow(() -> ServiceExceptions.notFound("No active email credentials found for business"));
    }
    private JavaMailSender createMailSender(EmailCredentials credentials) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(credentials.getSmtpHost());
        mailSender.setPort(credentials.getSmtpPort());
        mailSender.setUsername(credentials.getUsername());
        mailSender.setPassword(credentials.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.from", credentials.getFromEmail()); // Set default from address
        props.put("mail.from", credentials.getFromEmail()); // Set default from address
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "true"); // temporarily enable for troubleshooting

        if (credentials.getSmtpPort() == 465) {
            // SSL mode
            props.put("mail.smtp.ssl.enable", "true");
        } else if (credentials.getSmtpPort() == 587) {
            // STARTTLS mode
            props.put("mail.smtp.starttls.enable", "true");
        }

        return mailSender;
    }

    private String replaceVariables(String text, Map<String, String> variables) {
        if (variables == null || text == null) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private EmailTemplateResponse mapToTemplateResponse(EmailTemplate template) {
        return EmailTemplateResponse.builder()
            .id(template.getId())
            .templateName(template.getTemplateName())
            .subject(template.getSubject())
            .body(template.getBody())
            .templateType(template.getTemplateType())
            .variables(template.getVariables() != null ? template.getVariables() : new ArrayList<>())
            .isActive(template.getIsActive())
            .businessId(template.getBusinessId())
            .build();
    }

    /**
     * Converts technical email error messages to user-friendly messages
     */
    private String getUserFriendlyEmailError(String technicalMessage) {
        if (technicalMessage == null) {
            return "Failed to send email. Please try again later or contact support.";
        }

        String message = technicalMessage.toLowerCase();

        if (message.contains("sender address rejected") || message.contains("not owned by user")) {
            return "Unable to send email. Please contact support.";
        } else if (message.contains("invalid address") || message.contains("recipient rejected")
                || message.contains("mailbox not found") || message.contains("user unknown")) {
            return "The recipient email address is invalid. Please check and try again.";
        } else if (message.contains("authentication") || message.contains("535") || message.contains("login")) {
            return "Email service is temporarily unavailable. Please try again later.";
        } else if (message.contains("connection") || message.contains("timeout") || message.contains("unreachable")) {
            return "Email service is temporarily unavailable. Please try again later.";
        } else if (message.contains("no active email credentials")) {
            return "Email service is not configured. Please contact support.";
        } else {
            return "Failed to send email. Please try again later or contact support.";
        }
    }
}
