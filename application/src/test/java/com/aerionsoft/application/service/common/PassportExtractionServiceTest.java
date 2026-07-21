package com.aerionsoft.application.service.common;

import com.aerionsoft.application.dto.PassportExtractionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class PassportExtractionServiceTest {

    private static final String TESSDATA = "C:/Program Files/Tesseract-OCR/tessdata";

    @Mock
    private R2FileService r2FileService;

    private PassportExtractionService service;

    static boolean tesseractAvailable() {
        return Files.isDirectory(Path.of(TESSDATA));
    }

    @BeforeEach
    void setUp() throws IOException {
        service = new PassportExtractionService(r2FileService, tesseractAvailable(), TESSDATA);
        doReturn("https://example.test/passports/test.jpeg")
                .when(r2FileService).uploadRaw(any(), anyString());
    }

    @Test
    @EnabledIf("tesseractAvailable")
    void extractAsiferRahmanPassport() throws IOException {
        byte[] image = loadTestImage("passports/asifer-rahman.png");
        PassportExtractionResponse result = service.extractAndUpload(
                new MockMultipartFile("file", "asifer-rahman.png", "image/png", image));

        assertTrue(result.isExtracted(), () -> "Expected extracted=true, raw=" + result.getRawExtraction());
        assertEquals("A18102345", result.getPassportNumber());
        assertEquals("RAHMAN", result.getSurname());
        assertEquals("ASIFER", result.getGivenNames());
        assertEquals("BGD", result.getNationality());
        assertEquals("M", result.getGender());
        assertEquals("2000-11-25", result.getDateOfBirthIso());
        assertEquals("2025-02-20", result.getDateOfIssueIso());
        assertEquals("2035-02-19", result.getDateOfExpiryIso());
        assertNotNull(result.getMrzLine2());
        assertTrue(result.getMrzLine2().startsWith("A18102345"));
    }

    @Test
    @EnabledIf("tesseractAvailable")
    void extractMstSimaBegumPassport() throws IOException {
        byte[] image = loadTestImage("passports/mst-sima-begum.png");
        PassportExtractionResponse result = service.extractAndUpload(
                new MockMultipartFile("file", "mst-sima-begum.png", "image/png", image));

        assertTrue(result.isExtracted(), () -> "Expected extracted=true, raw=" + result.getRawExtraction());
        assertEquals("A18370350", result.getPassportNumber());
        assertEquals("BEGUM", result.getSurname());
        assertEquals("MST SIMA", result.getGivenNames());
        assertEquals("BGD", result.getNationality());
        assertEquals("F", result.getGender());
        assertEquals("1995-07-07", result.getDateOfBirthIso());
        assertEquals("2025-04-10", result.getDateOfIssueIso());
        assertEquals("2035-04-09", result.getDateOfExpiryIso());
        assertNotNull(result.getMrzLine2());
        assertTrue(result.getMrzLine2().startsWith("A18370350"));
    }

    @Test
    @EnabledIf("tesseractAvailable")
    void extractTaisirKhanPassport() throws IOException {
        byte[] image = loadTestImage("passports/taisir-khan.png");
        PassportExtractionResponse result = service.extractAndUpload(
                new MockMultipartFile("file", "taisir-khan.png", "image/png", image));

        assertTrue(result.isExtracted(), () -> "Expected extracted=true, raw=" + result.getRawExtraction());
        assertEquals("A17724087", result.getPassportNumber());
        assertEquals("KHAN", result.getSurname());
        assertEquals("TAISIR", result.getGivenNames());
        assertEquals("BGD", result.getNationality());
        assertEquals("M", result.getGender());
        assertEquals("1999-04-05", result.getDateOfBirthIso());
        assertEquals("2025-01-28", result.getDateOfIssueIso());
        assertEquals("2035-01-27", result.getDateOfExpiryIso());
        assertNotNull(result.getMrzLine2());
        assertTrue(result.getMrzLine2().startsWith("A17724087"));
    }

    @Test
    @EnabledIf("tesseractAvailable")
    void extractJahangirSingleNamePassport() throws IOException {
        byte[] image = loadTestImage("passports/jahangir-single-name.png");
        PassportExtractionResponse result = service.extractAndUpload(
                new MockMultipartFile("file", "jahangir-single-name.png", "image/png", image));

        assertTrue(result.isExtracted(), () -> "Expected extracted=true, raw=" + result.getRawExtraction());
        assertEquals("A06893248", result.getPassportNumber());
        assertEquals("JAHANGIR", result.getSurname());
        assertNull(result.getGivenNames(),
                () -> "Single-name passport should have no givenNames, got: " + result.getGivenNames());
        assertEquals("BGD", result.getNationality());
        assertEquals("M", result.getGender());
        assertEquals("1976-03-16", result.getDateOfBirthIso());
        assertEquals("2023-01-30", result.getDateOfIssueIso());
        assertEquals("2033-01-29", result.getDateOfExpiryIso());
    }

    private byte[] loadTestImage(String resourcePath) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + resourcePath);
            }
            return stream.readAllBytes();
        }
    }
}
