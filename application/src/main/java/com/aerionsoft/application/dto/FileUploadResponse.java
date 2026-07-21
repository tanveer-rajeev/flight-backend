package com.aerionsoft.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResponse {
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String uploadedBy;
    private String type;
}
