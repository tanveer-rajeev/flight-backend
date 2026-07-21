package com.aerionsoft.application.controller.common;

import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.BaseResponse;
import com.aerionsoft.application.dto.PassportExtractionResponse;
import com.aerionsoft.application.service.common.PassportExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Passport image upload + Tesseract OCR extraction.
 *
 * POST /api/passport/extract
 */
@RestController
@Validated
@RequestMapping("/api/passport")
@CrossOrigin(origins = "*")
public class PassportExtractionController extends BaseController {

    @Autowired
    private PassportExtractionService passportExtractionService;

    @PostMapping("/extract")
    public ResponseEntity<BaseResponse<PassportExtractionResponse>> extractPassport(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        PassportExtractionResponse result = passportExtractionService.extractAndUpload(file);

        String message = result.isExtracted()
                ? "Passport data extracted successfully"
                : "Image uploaded but extraction returned unstructured data — see rawExtraction";

        return ResponseEntity.ok(BaseResponse.ok(message, result));
    }
}
