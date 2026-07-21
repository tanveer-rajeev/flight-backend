package com.aerionsoft.application.service.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // Allowed MIME types
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp"
    );
    // Max file size: 2 MB
    private static final long MAX_SIZE = 20 * 1024 * 1024;

    public String saveFile(MultipartFile file, String username) throws IOException {
        // Validate file type
        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file type: only images are allowed.");
        }
        // Validate file size
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File too large (max 2MB).");
        }
        // Create unique filename using hash
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf('.')))
                .orElse("");
        String hash = UUID.randomUUID().toString().replace("-", "");
        String filename = username.replaceAll("[^a-zA-Z0-9]", "_") + "_" + hash + ext;

        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        Path filepath = dir.resolve(filename);
        Files.copy(file.getInputStream(), filepath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}