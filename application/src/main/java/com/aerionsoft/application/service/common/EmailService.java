package com.aerionsoft.application.service.common;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.entity.email.EmailCredentials;
import com.aerionsoft.application.entity.email.EmailLogs;
import com.aerionsoft.application.exception.EmailException;
import com.aerionsoft.application.repository.email.EmailCredentialsRepository;
import com.aerionsoft.application.repository.email.EmailLogsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service("generalEmailService")
public class EmailService {

    @Autowired
    private EmailCredentialsRepository emailCredentialsRepository;

    @Autowired
    private EmailLogsRepository emailLogsRepository;

    // Backward compatible - defaults to businessId = 0
    public void sendEmail(String to, String subject, String text) {
        sendEmail(to, subject, text, 0L);
    }

    // New method with businessId support
    public void sendEmail(String to, String subject, String text, Long businessId) {
        businessId = businessId != null ? businessId : 0L;

        EmailLogs emailLog = EmailLogs.builder()
                .toEmail(to)
                .subject(subject)
                .body(text)
                .status("PENDING")
                .businessId(businessId)
                .build();

        try {
            EmailCredentials credentials = emailCredentialsRepository.findActiveCredentialsByBusinessId(businessId)
                    .orElseThrow(() -> ServiceExceptions.emailError("No active email credentials found for business"));

            JavaMailSender mailSender = createMailSender(credentials);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(credentials.getFromEmail());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);

            // Log successful email
            emailLog.setStatus("SENT");
            emailLog.setSentAt(UserDateTimeUtil.now());
            emailLogsRepository.save(emailLog);

        } catch (Exception e) {
            // Log failed email
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailLogsRepository.save(emailLog);
            throw EmailException.fromMailException(e);
        }
    }

    // Backward compatible - defaults to businessId = 0
    public void sendEmail(String to, String subject, String text, String[] ccEmails, String[] bccEmails) {
        sendEmail(to, subject, text, ccEmails, bccEmails, 0L);
    }

    // New method with businessId support
    public void sendEmail(String to, String subject, String text, String[] ccEmails, String[] bccEmails, Long businessId) {
        businessId = businessId != null ? businessId : 0L;

        EmailLogs emailLog = EmailLogs.builder()
                .toEmail(to)
                .ccEmails(ccEmails)
                .bccEmails(bccEmails)
                .subject(subject)
                .body(text)
                .status("PENDING")
                .businessId(businessId)
                .build();

        try {
            EmailCredentials credentials = emailCredentialsRepository.findActiveCredentialsByBusinessId(businessId)
                    .orElseThrow(() -> ServiceExceptions.emailError("No active email credentials found for business"));


            JavaMailSender mailSender = createMailSender(credentials);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(credentials.getFromEmail());
            message.setTo(to);
            if (ccEmails != null && ccEmails.length > 0) {
                message.setCc(ccEmails);
            }
            if (bccEmails != null && bccEmails.length > 0) {
                message.setBcc(bccEmails);
            }
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);

            // Log successful email
            emailLog.setStatus("SENT");
            emailLog.setSentAt(UserDateTimeUtil.now());
            emailLogsRepository.save(emailLog);

        } catch (Exception e) {
            // Log failed email
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailLogsRepository.save(emailLog);
            throw EmailException.fromMailException(e);
        }
    }

    // Backward compatible - defaults to businessId = 0
    public void sendOtp(String email, String otp) {
        sendOtp(email, otp, 0L);
    }

    // New method with businessId support
    public void sendOtp(String email, String otp, Long businessId) {
        String subject = "Your OTP Code";
        String text = "Your OTP is: " + otp + "\n\nThis OTP will expire in 5 minutes.";
        sendEmail(email, subject, text, businessId);
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

}
