package com.aerionsoft.application.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TesseractDataPathResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvePrefersValidConfiguredPath() throws Exception {
        Path tessdata = tempDir.resolve("custom-tessdata");
        Files.createDirectories(tessdata);
        Files.createFile(tessdata.resolve("eng.traineddata"));

        String resolved = TesseractDataPathResolver.resolve(tessdata.toString());

        assertEquals(tessdata.toString(), resolved);
    }

    @Test
    void resolveFallsBackWhenConfiguredPathInvalid() throws Exception {
        Path fallback = tempDir.resolve("fallback-tessdata");
        Files.createDirectories(fallback);
        Files.createFile(fallback.resolve("eng.traineddata"));

        String resolved = TesseractDataPathResolver.resolve("/nonexistent/tessdata");

        if (TesseractDataPathResolver.isValidTessDataDir("/usr/share/tesseract/tessdata")
                || TesseractDataPathResolver.isValidTessDataDir("/usr/share/tessdata")
                || TesseractDataPathResolver.isValidTessDataDir("C:/Program Files/Tesseract-OCR/tessdata")) {
            assertTrue(TesseractDataPathResolver.isValidTessDataDir(resolved));
        } else {
            assertEquals("/nonexistent/tessdata", resolved);
        }
    }

    @Test
    void isValidTessDataDirRequiresEngTrainedData() throws Exception {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        assertFalse(TesseractDataPathResolver.isValidTessDataDir(emptyDir.toString()));
        assertFalse(TesseractDataPathResolver.isValidTessDataDir(null));
        assertFalse(TesseractDataPathResolver.isValidTessDataDir("  "));
    }
}
