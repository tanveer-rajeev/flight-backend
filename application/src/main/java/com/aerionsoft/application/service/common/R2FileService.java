package com.aerionsoft.application.service.common;

import com.aerionsoft.application.exception.ServiceExceptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class R2FileService {

    private final S3Client r2Client;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicBaseUrl;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/webm",
            "audio/ogg",
            "audio/mpeg",
            "audio/mp3",
            "audio/mp4",
            "audio/m4a",
            "audio/x-m4a",
            "audio/wav",
            "audio/x-wav",
            "audio/aac",
            // Browsers sometimes omit a precise type for recorded blobs
            "application/octet-stream"
    );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            // PDF
            "application/pdf",
            // Word
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            // Excel
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            // PowerPoint
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            // CSV / text
            "text/csv", "application/csv", "text/plain",
            // Images
            "image/png", "image/jpeg", "image/jpg",
            // Generic binary — browsers/Postman sometimes report unknown files as this
            "application/octet-stream"
    );

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;     // 5 MB — matches /api/files contract
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final long MAX_AUDIO_SIZE = 5 * 1024 * 1024;     // 5 MB voice notes
    /** Maximum voice-note length in seconds (1 minute). */
    public static final int MAX_AUDIO_DURATION_SECONDS = 60;

    public R2FileService(@Qualifier("r2Client") S3Client r2Client) {
        this.r2Client = r2Client;
    }

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        validateImage(file);
        return upload(file, folder + "/images/");
    }

    public String uploadDocument(MultipartFile file, String folder) throws IOException {
        validateDocument(file);
        return upload(file, folder + "/documents/");
    }

    /** Chat / voice-note uploads. Max 5 MB and {@link #MAX_AUDIO_DURATION_SECONDS}. */
    public String uploadAudio(MultipartFile file, String folder) throws IOException {
        return uploadAudio(file, folder, null);
    }

    public String uploadAudio(MultipartFile file, String folder, Double durationSeconds) throws IOException {
        validateAudio(file, durationSeconds);
        String ext = resolveAudioExtension(file);
        return upload(file, folder + "/audio/", ext);
    }

    /**
     * Upload any file to R2 under the given key prefix. Returns the public URL.
     * Used internally for passport images before AI extraction.
     */
    public String uploadRaw(MultipartFile file, String keyPrefix) throws IOException {
        return upload(file, keyPrefix, null);
    }

    private String upload(MultipartFile file, String folderPath) throws IOException {
        return upload(file, folderPath, null);
    }

    private String upload(MultipartFile file, String folderPath, String forcedExtension) throws IOException {
        try {
            String key = folderPath + buildUniqueFileName(file.getOriginalFilename(), forcedExtension);

            String contentType = normalizeContentType(file.getContentType());
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            r2Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return buildPublicUrl(key);
        } catch (S3Exception e) {
            String detail = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            throw ServiceExceptions.fileError("Failed to upload file to R2: " + detail, e);
        } catch (Exception e) {
            throw ServiceExceptions.fileError("Failed to upload file to R2: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            r2Client.deleteObject(request);
        } catch (S3Exception e) {
            throw ServiceExceptions.fileError("Failed to delete file from R2", e);
        }
    }

    /**
     * Extracts the object key from a full R2 public URL.
     * Returns null if the URL does not belong to this bucket's public URL.
     */
    public String extractKeyFromUrl(String url) {
        if (url == null) return null;

        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        if (url.startsWith(base)) {
            return url.substring(base.length());
        }

        // Fallback: path-style URL containing bucket name
        String marker = "/" + bucketName + "/";
        int idx = url.indexOf(marker);
        if (idx >= 0) {
            return url.substring(idx + marker.length());
        }

        return null;
    }

    public String buildPublicUrl(String key) {
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        return base + key;
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File cannot be empty");

        String ct = file.getContentType();
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct)) {
            throw new IllegalArgumentException("Invalid image type. Allowed: " + String.join(", ", ALLOWED_IMAGE_TYPES));
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image too large. Maximum size is 5 MB");
        }
    }

    private void validateDocument(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File cannot be empty");

        String ct = file.getContentType();
        if (ct == null || !ALLOWED_DOCUMENT_TYPES.contains(ct)) {
            throw new IllegalArgumentException(
                "Unsupported file type '" + ct + "'. Allowed: PDF, Word (doc/docx), " +
                "Excel (xls/xlsx), PowerPoint (ppt/pptx), CSV, plain text, PNG, JPEG");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException("Document too large. Maximum size is 10 MB");
        }
    }

    private void validateAudio(MultipartFile file, Double durationSeconds) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file cannot be empty");
        }
        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw new IllegalArgumentException("Audio too large. Maximum size is 5 MB");
        }

        if (durationSeconds == null) {
            throw new IllegalArgumentException(
                    "durationSeconds is required (voice notes max " + MAX_AUDIO_DURATION_SECONDS + " seconds)");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be greater than 0");
        }
        if (durationSeconds > MAX_AUDIO_DURATION_SECONDS) {
            throw new IllegalArgumentException(
                    "Voice note too long. Maximum is " + MAX_AUDIO_DURATION_SECONDS + " seconds (1 minute)");
        }

        String ct = normalizeContentType(file.getContentType());
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        boolean typeOk = ct != null && isAllowedAudioContentType(ct);
        boolean extOk = hasAllowedAudioExtension(name);

        // MediaRecorder often sends audio/webm;codecs=opus or even video/webm
        if (!typeOk && !extOk) {
            throw new IllegalArgumentException(
                    "Invalid audio type '" + file.getContentType() + "'. Allowed: webm, ogg, mp3, m4a, wav, aac");
        }
    }

    private static boolean isAllowedAudioContentType(String ct) {
        if (ALLOWED_AUDIO_TYPES.contains(ct)) {
            return true;
        }
        // Prefix matches for codec suffixes already stripped by normalizeContentType
        return ct.startsWith("audio/webm")
                || ct.startsWith("audio/ogg")
                || ct.startsWith("audio/mpeg")
                || ct.startsWith("audio/mp4")
                || ct.startsWith("audio/wav")
                || ct.startsWith("audio/aac")
                || ct.startsWith("audio/x-m4a")
                || ct.equals("video/webm"); // Chrome audio-only recordings sometimes use this
    }

    private static boolean hasAllowedAudioExtension(String filename) {
        return filename.endsWith(".webm") || filename.endsWith(".ogg") || filename.endsWith(".mp3")
                || filename.endsWith(".m4a") || filename.endsWith(".wav") || filename.endsWith(".aac");
    }

    /** Prefer filename extension; otherwise infer from content-type (Blob uploads often have no name). */
    private static String resolveAudioExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".") && !name.equalsIgnoreCase("blob")) {
            String ext = name.substring(name.lastIndexOf('.')).toLowerCase();
            if (ext.matches("\\.(webm|ogg|mp3|m4a|wav|aac)")) {
                return ext;
            }
        }
        String ct = normalizeContentType(file.getContentType());
        if (ct == null) {
            return ".webm";
        }
        if (ct.contains("webm")) return ".webm";
        if (ct.contains("ogg")) return ".ogg";
        if (ct.contains("mpeg") || ct.equals("audio/mp3")) return ".mp3";
        if (ct.contains("mp4") || ct.contains("m4a")) return ".m4a";
        if (ct.contains("wav")) return ".wav";
        if (ct.contains("aac")) return ".aac";
        return ".webm";
    }

    /** Strip codec / parameter suffixes: {@code audio/webm;codecs=opus} → {@code audio/webm}. */
    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String ct = contentType.trim().toLowerCase();
        int semi = ct.indexOf(';');
        if (semi >= 0) {
            ct = ct.substring(0, semi).trim();
        }
        return ct;
    }

    private String buildUniqueFileName(String originalFilename) {
        return buildUniqueFileName(originalFilename, null);
    }

    private String buildUniqueFileName(String originalFilename, String forcedExtension) {
        if (forcedExtension != null && !forcedExtension.isBlank()) {
            String ext = forcedExtension.startsWith(".") ? forcedExtension : "." + forcedExtension;
            return UUID.randomUUID() + ext;
        }
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")
                && !originalFilename.equalsIgnoreCase("blob")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + ext;
    }
}
