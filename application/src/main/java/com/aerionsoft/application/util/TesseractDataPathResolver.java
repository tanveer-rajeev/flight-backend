package com.aerionsoft.application.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Resolves the Tesseract {@code tessdata} directory for the current OS.
 * An explicit {@code tesseract.datapath} property takes precedence when valid;
 * otherwise common platform paths are tried (Linux package install, Windows installer, Homebrew).
 */
public final class TesseractDataPathResolver {

    private static final Logger log = LoggerFactory.getLogger(TesseractDataPathResolver.class);
    private static final String ENG_TRAINED_DATA = "eng.traineddata";

    private TesseractDataPathResolver() {
    }

    public static String resolve(String configuredPath) {
        if (isValidTessDataDir(configuredPath)) {
            log.info("Using configured Tesseract tessdata path: {}", configuredPath);
            return configuredPath;
        }

        if (configuredPath != null && !configuredPath.isBlank()) {
            log.warn(
                    "Configured tesseract.datapath '{}' is missing {}; auto-detecting platform default",
                    configuredPath,
                    ENG_TRAINED_DATA);
        }

        for (String candidate : platformCandidates()) {
            if (isValidTessDataDir(candidate)) {
                log.info("Auto-detected Tesseract tessdata path: {}", candidate);
                return candidate;
            }
        }

        log.warn(
                "No valid Tesseract tessdata directory found. Set tesseract.datapath or install tesseract-langpack-eng.");
        return configuredPath != null ? configuredPath.trim() : "";
    }

    static boolean isValidTessDataDir(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return Files.isRegularFile(Path.of(path.trim(), ENG_TRAINED_DATA));
    }

    private static List<String> platformCandidates() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return List.of("C:/Program Files/Tesseract-OCR/tessdata");
        }
        if (os.contains("mac")) {
            return List.of(
                    "/opt/homebrew/share/tessdata",
                    "/usr/local/share/tessdata");
        }
        return List.of(
                "/usr/local/share/tessdata",
                "/usr/share/tesseract/tessdata",
                "/usr/share/tessdata");
    }
}
