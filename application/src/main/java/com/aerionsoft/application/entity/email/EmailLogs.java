package com.aerionsoft.application.entity.email;

import com.aerionsoft.application.util.UserDateTimeUtil;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "cc_emails", columnDefinition = "TEXT[]")
    private String[] ccEmails;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "bcc_emails", columnDefinition = "TEXT[]")
    private String[] bccEmails;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "template_id")
    private Long templateId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "attachment_urls", columnDefinition = "TEXT[]")
    private String[] attachmentUrls;

    @Column(nullable = false, length = 50)
    private String status; // SENT, FAILED, PENDING

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "business_id", nullable = true)
    @Builder.Default
    private Long businessId = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @PrePersist
    public void prePersist() {
        this.createdAt = UserDateTimeUtil.now();
    }
}
