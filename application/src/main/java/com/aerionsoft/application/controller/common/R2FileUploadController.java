package com.aerionsoft.application.controller.common;

import com.aerionsoft.application.controller.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Duplicate R2 upload routes — disabled; use {@link FileUploadController} at {@code /api/files} instead.
 */
// @RestController
@Validated
// @RequestMapping("/api/r2/files")
@CrossOrigin(origins = "*")
public class R2FileUploadController extends BaseController {

/*
    @Autowired
    private R2FileService r2FileService;

    @PostMapping("/upload/image")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            Authentication authentication) throws IOException {

        String fileUrl = r2FileService.uploadImage(file, folder);

        FileUploadResponse data = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadedBy(authentication.getName())
                .type("image")
                .build();

        return ResponseEntity.ok(BaseResponse.ok("Image uploaded successfully", data));
    }

    @PostMapping("/upload/document")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            Authentication authentication) throws IOException {

        String fileUrl = r2FileService.uploadDocument(file, folder);

        FileUploadResponse data = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadedBy(authentication.getName())
                .type("document")
                .build();

        return ResponseEntity.ok(BaseResponse.ok("Document uploaded successfully", data));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponse<Map<String, String>>> deleteFile(
            @RequestParam("fileUrl") String fileUrl,
            Authentication authentication) {

        String key = r2FileService.extractKeyFromUrl(fileUrl);
        if (key == null) {
            throw new IllegalArgumentException("Invalid or unrecognised R2 file URL");
        }

        r2FileService.deleteFile(key);

        return ResponseEntity.ok(BaseResponse.ok(
                "File deleted successfully",
                Map.of("deletedBy", authentication.getName(), "key", key)));
    }
*/
}
