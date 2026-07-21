package com.aerionsoft.application.entity.visa;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "visa_application_document")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisaApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonBackReference
    private VisaApplication application;

    @Column(name = "doc_type", nullable = false)
    private String docType;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = UserDateTimeUtil.now();
}
