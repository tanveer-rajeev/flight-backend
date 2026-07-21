package com.aerionsoft.application.controller.common;

import com.aerionsoft.application.controller.BaseController;

import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.FileUploadResponse;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.service.common.R2FileService;
import com.aerionsoft.application.service.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileUploadController extends BaseController {

    @Autowired
    private R2FileService fileService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Upload an image file (PNG, JPEG, GIF, WebP).
     * Max size: 5MB. Agency users or admins.
     */
    @PostMapping("/upload/image")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            Authentication authentication) throws IOException {

        requireAgencyOrAdminUploader(authentication);
        String fileUrl = fileService.uploadImage(file, folder);
        FileUploadResponse data = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadedBy(authentication.getName())
                .build();

        return ResponseEntity.ok(BaseResponse.ok("Image uploaded successfully", data));
    }

    /**
     * Upload a document file (CSV, PDF, DOC, DOCX)
     * Max size: 10MB
     */
    @PostMapping("/upload/document")
//    @PreAuthorize("@permissionService.hasPermission(authentication, 'upload-document')") // admin or user
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            Authentication authentication) throws IOException {

        String fileUrl = fileService.uploadDocument(file, folder);
        FileUploadResponse data = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadedBy(authentication.getName())
                .build();

        return ResponseEntity.ok(BaseResponse.ok("Document uploaded successfully", data));
    }

    /**
     * Upload a voice / audio note for live chat (webm, ogg, mp3, m4a, wav, aac).
     * Max size: 5MB. Max duration: {@code durationSeconds} ≤ 60.
     * Agency users or admins. Use folder {@code live-chat}.
     */
    @PostMapping("/upload/audio")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "live-chat") String folder,
            @RequestParam(value = "durationSeconds", required = false) Double durationSeconds,
            Authentication authentication) throws IOException {

        requireAgencyOrAdminUploader(authentication);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required (form field name must be 'file')");
        }

        String fileUrl = fileService.uploadAudio(file, folder, durationSeconds);
        String uploadedBy = authentication.getName();
        FileUploadResponse data = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadedBy(uploadedBy)
                .type("audio")
                .build();

        return ResponseEntity.ok(BaseResponse.ok("Audio uploaded successfully", data));
    }

    /**
     * Upload CSV file specifically for data import
     * Max size: 10MB
     */
    @PostMapping("/upload/csv")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'csv-import')")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "csv-imports") String folder,
            Authentication authentication) throws IOException {

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.contains("csv") && !contentType.contains("text/plain"))) {
            throw new IllegalArgumentException("File must be a CSV file");
        }

        String fileUrl = fileService.uploadDocument(file, folder);
        FileUploadResponse data = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadedBy(authentication.getName())
                .type("csv")
                .build();

        return ResponseEntity.ok(BaseResponse.ok("CSV file uploaded successfully", data));
    }

    /**
     * Delete a file from object storage (Cloudflare R2)
     */
    @DeleteMapping("/delete")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'delete-file')")
    public ResponseEntity<BaseResponse<Map<String, String>>> deleteFile(
            @RequestParam("fileUrl") String fileUrl,
            Authentication authentication) {

        String key = fileService.extractKeyFromUrl(fileUrl);
        if (key == null) {
            throw new IllegalArgumentException("Invalid file URL");
        }

        fileService.deleteFile(key);
        return ResponseEntity.ok(BaseResponse.ok(
                "File deleted successfully",
                Map.of("deletedBy", authentication.getName())));
    }

    /**
     * Get file information
     */
    @GetMapping("/info")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'view-file-info')") // admin or user
    public ResponseEntity<BaseResponse<Map<String, Object>>> getFileInfo(
            @RequestParam("fileUrl") String fileUrl) {

        String key = fileService.extractKeyFromUrl(fileUrl);
        if (key == null) {
            throw new IllegalArgumentException("Invalid file URL");
        }

        Map<String, Object> data = Map.of(
                "fileUrl", fileUrl,
                "key", key,
                "accessible", true
        );
        return ResponseEntity.ok(BaseResponse.ok(data));
    }

    /** Admins, or mother agency / staff under an agency mother account. */
    private void requireAgencyOrAdminUploader(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String provider = details.getProvider();
        if ("admin".equalsIgnoreCase(provider)) {
            return;
        }
        if (!"user".equalsIgnoreCase(provider)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Only agency or admin users can upload images and audio");
        }
        User user = userRepository.findById(details.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", details.getId()));
        User root = user.getParentUser() != null ? user.getParentUser() : user;
        if (!root.isAgency()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Only agency or admin users can upload images and audio");
        }
    }
}
