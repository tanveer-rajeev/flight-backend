package com.aerionsoft.application.service.common;

import com.aerionsoft.application.dto.PassportExtractionResponse;
import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.util.TesseractDataPathResolver;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PassportExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PassportExtractionService.class);
    private static final int MRZ_LINE_LENGTH = 44;
    private static final DateTimeFormatter VISUAL_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd MMM uuuu")
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter VISUAL_DATE_SHORT_YEAR_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd MMM uu")
            .toFormatter(Locale.ENGLISH);

    private static final Pattern PASSPORT_NUMBER_LABEL_PATTERN = Pattern.compile(
            "(?i)(?:passport\\s*(?:no|number|#)?|document\\s*(?:no|number)?)\\s*[:\\-]?\\s*([A-Z][0-9]{8})");
    private static final Pattern DATE_WITH_LABEL_PATTERN = Pattern.compile(
            "(?i)(date\\s*of\\s*birth|birth|dob|date\\s*of\\s*expiry|expiry|date\\s*of\\s*issue|issue)\\s*[:\\-]?\\s*"
                    + "(\\d{2}[\\-/\\.][0-9]{2}[\\-/\\.][0-9]{2,4}|\\d{2}\\s+[A-Z]{3}\\s+\\d{2,4})");
    private static final Pattern STANDALONE_DATE_PATTERN = Pattern.compile(
            "\\b(\\d{2}\\s+(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\s+\\d{4})\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPACT_ALPHA_DATE_PATTERN = Pattern.compile(
            "(\\d{2})(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(\\d{4})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SURNAME_PATTERN = Pattern.compile("(?i)\\bsurname\\b\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern GIVEN_NAME_PATTERN = Pattern.compile("(?i)\\bgiven\\s*name(?:s)?\\b\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern NATIONALITY_PATTERN = Pattern.compile("(?i)\\bnationality\\b\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern PLACE_OF_BIRTH_PATTERN = Pattern.compile("(?i)\\bplace\\s*of\\s*birth\\b\\s*[:\\-]?\\s*(.+)$");
    private static final Pattern SEX_PATTERN = Pattern.compile("(?i)\\b(?:sex|gender)\\b\\s*[:\\-]?\\s*([MFX])\\b");
    private static final Pattern BGD_PASSPORT_NUMBER_PATTERN = Pattern.compile("\\b([A-Z][0-9]{8})\\b");
    private static final Pattern MRZ_EMBEDDED_PASSPORT_PATTERN = Pattern.compile("([A-Z][0-9]{8})\\dBGD");
    private static final Pattern MRZ_LINE_2_CANDIDATE = Pattern.compile("[A-Z0-9<]{44}");
    private static final Pattern MRZ_LINE_2_BGD_CORE = Pattern.compile(
            "([A-Z][0-9]{8})([0-9])BGD([0-9]{6})([0-9])([A-Z0-9<])([0-9]{6})([0-9])");
    private static final Pattern MRZ_LINE_1_BGD = Pattern.compile("P<BGD([A-Z]+)<<([A-Z<]+)");
    private static final Pattern MRZ_LOOSE_NAME_PATTERN = Pattern.compile("([A-Z]{3,})[<K]+([A-Z][A-Z<]{2,})");
    private static final Pattern LOOSE_SEX_PATTERN = Pattern.compile("(?i)\\b(?:sex|gender)\\b[^\\n]{0,30}?\\b([MFX])\\b");
    private static final Set<String> MRZ_NAME_NOISE = Set.of(
            "PASSPORT", "BANGLADESHI", "BANGLADESH", "REPUBLIC", "PEOPLES", "PEOPLE",
            "COUNTRY", "NATIONALITY", "HOLDER", "DATEOF", "BIRTH", "ISSUE", "EXPIRY",
            "AUTHORITY", "DIP", "DHAKA", "PREVIOUS", "PERSONAL", "NUMBER", "CODE",
            "SURNAME", "GIVEN", "PLACE", "SEX", "TYPE", "SIGNATURE", "PEOPLED");
    private static final Set<String> GIVEN_NAME_NOISE = Set.of(
            "NAME", "GIVEN", "NAM", "NAMES", "WAR", "NAT", "DATE", "SEX", "TYPE", "CODE",
            "PASSPORT", "BIRTH", "ISSUE", "EXPIRY", "HOLDER", "SIGNATURE", "NATIONALITY",
            "BANGLADESHI", "BANGLADESH", "SURNAME", "PLACE", "PERSONAL", "NUMBER", "AUTHORITY");

    private final R2FileService r2FileService;
    private final ITesseract fullPageTesseract;
    private final ITesseract mrzTesseract;

    @Value("${passport.ocr-max-dimension:2200}")
    private int ocrMaxDimension;

    public PassportExtractionService(
            R2FileService r2FileService,
            @Value("${tesseract.enabled:false}") boolean tesseractEnabled,
            @Value("${tesseract.datapath:}") String tessDataPath) {

        this.r2FileService = r2FileService;
        if (tesseractEnabled) {
            String resolvedTessDataPath = TesseractDataPathResolver.resolve(tessDataPath);
            this.fullPageTesseract = createTesseract(resolvedTessDataPath, 6, null);
            this.mrzTesseract = createTesseract(resolvedTessDataPath, 7,
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<");
            validateNativeLibraries(this.fullPageTesseract);
        } else {
            this.fullPageTesseract = null;
            this.mrzTesseract = null;
        }
    }

    private static void validateNativeLibraries(ITesseract tesseract) {
        try {
            BufferedImage probe = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
            tesseract.doOCR(probe);
            log.info("Tesseract native libraries (leptonica/tesseract) loaded successfully");
        } catch (TesseractException ignored) {
            log.info("Tesseract native libraries (leptonica/tesseract) loaded successfully");
        } catch (LinkageError e) {
            log.error(
                    "Tesseract/Leptonica native libraries failed to load. "
                            + "Use ./start.sh (bundled leptonica) and remove /usr/lib64/libleptonica.so symlink on EL9. "
                            + "See docs/tesseract-centos-setup.md",
                    e);
            throw new IllegalStateException(
                    "Passport OCR native libraries unavailable. See docs/tesseract-centos-setup.md",
                    e);
        }
    }

    public PassportExtractionResponse extractAndUpload(MultipartFile file) throws IOException {
        log.info("Starting passport extraction for file: {}", file.getOriginalFilename());

        validatePassportImage(file);

        String imageUrl = r2FileService.uploadRaw(file, "passports/");
        log.info("Image uploaded to R2: {}", imageUrl);

        byte[] imageBytes = file.getBytes();
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (source == null) {
            throw ServiceExceptions.microservice("Could not decode passport image.");
        }

        String fullPageOcr = ocrFullPage(source);
        String mrzOcr = ocrMrzRegion(source);
        String combinedOcr = fullPageOcr + "\n" + mrzOcr;
        log.info("Tesseract OCR completed - full page: {} chars, MRZ region: {} chars",
                fullPageOcr.length(), mrzOcr.length());

        ExtractedPassportFields fields = extractPassportFields(fullPageOcr, mrzOcr);
        boolean extracted = hasMeaningfulExtraction(fields);

        PassportExtractionResponse.PassportExtractionResponseBuilder builder = PassportExtractionResponse.builder()
                .imageUrl(imageUrl)
                .surname(fields.surname())
                .givenNames(fields.givenNames())
                .passportNumber(fields.passportNumber())
                .nationality(fields.nationality())
                .dateOfBirth(fields.dateOfBirth())
                .dateOfBirthIso(normalizeDateToIso(fields.dateOfBirth(), DateKind.BIRTH))
                .dateOfExpiry(fields.dateOfExpiry())
                .dateOfExpiryIso(normalizeDateToIso(fields.dateOfExpiry(), DateKind.EXPIRY))
                .dateOfIssue(fields.dateOfIssue())
                .dateOfIssueIso(normalizeDateToIso(fields.dateOfIssue(), DateKind.ISSUE))
                .gender(fields.gender())
                .placeOfBirth(fields.placeOfBirth())
                .issuingCountry(fields.issuingCountry())
                .mrzLine1(fields.mrzLine1())
                .mrzLine2(fields.mrzLine2())
                .extracted(extracted);

        if (!extracted) {
            builder.rawExtraction(combinedOcr);
        }
        return builder.build();
    }

    private ITesseract createTesseract(String tessDataPath, int pageSegMode, String charWhitelist) {
        Tesseract instance = new Tesseract();
        instance.setLanguage("eng");
        instance.setPageSegMode(pageSegMode);
        instance.setOcrEngineMode(1);
        if (charWhitelist != null) {
            instance.setVariable("tessedit_char_whitelist", charWhitelist);
        }
        if (tessDataPath != null && !tessDataPath.isBlank()) {
            instance.setDatapath(tessDataPath);
        }
        return instance;
    }

    private String ocrFullPage(BufferedImage source) {
        try {
            return fullPageTesseract.doOCR(enhanceForOcr(source, ocrMaxDimension)).trim();
        } catch (TesseractException e) {
            log.error("Full-page Tesseract OCR failed", e);
            throw ServiceExceptions.microservice("Passport OCR failed. Ensure Tesseract is installed correctly.", e);
        } catch (LinkageError e) {
            throw ServiceExceptions.microservice(
                    "Passport OCR native libraries failed to load. "
                            + "Rebuild app.jar, use ./start.sh, remove libleptonica.so symlink on EL9. "
                            + "See docs/tesseract-centos-setup.md",
                    e);
        }
    }

    private String ocrMrzRegion(BufferedImage source) {
        try {
            BufferedImage mrzCrop = cropMrzRegion(source);
            BufferedImage enhanced = binarizeForMrz(enhanceForOcr(mrzCrop, Math.max(ocrMaxDimension, 2400)));
            return mrzTesseract.doOCR(enhanced).trim();
        } catch (Exception e) {
            log.warn("MRZ-region OCR failed, continuing with full-page OCR only", e);
            return "";
        }
    }

    private BufferedImage cropMrzRegion(BufferedImage source) {
        int height = source.getHeight();
        int width = source.getWidth();
        int cropTop = (int) (height * 0.78);
        int cropHeight = height - cropTop;
        return source.getSubimage(0, cropTop, width, Math.max(1, cropHeight));
    }

    private BufferedImage enhanceForOcr(BufferedImage source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);
        double scale = longest < maxDimension ? (double) maxDimension / longest : 1.0d;

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage grayscale = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayscale.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }
        return grayscale;
    }

    private BufferedImage binarizeForMrz(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = gray.getRGB(x, y) & 0xFF;
                binary.setRGB(x, y, rgb < 140 ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return binary;
    }

    private ExtractedPassportFields extractPassportFields(String fullPageOcr, String mrzOcr) {
        String combinedOcr = mrzOcr + "\n" + fullPageOcr;
        String compact = compactMrzText(combinedOcr);
        ExtractedPassportFields visual = extractVisualFields(combinedOcr);
        ValidMrz mrz = findValidMrz(combinedOcr);

        if (mrz != null) {
            ExtractedPassportFields fromMrz = enrichWithLooseMrzNames(
                    parseMrz(mrz.line1(), mrz.line2()), compact, mrz.line2());
            return mergeFields(visual, fromMrz);
        }

        ExtractedPassportFields looseMrz = extractLooseMrzFields(combinedOcr);
        if (hasMeaningfulExtraction(looseMrz)) {
            log.info("Using relaxed MRZ parsing after strict checksum match failed");
            return mergeFields(visual, looseMrz);
        }

        log.warn("No checksum-valid MRZ found; using visual OCR fields only");
        return mergeFields(visual, looseMrz);
    }

    private ValidMrz findValidMrz(String ocrText) {
        String compact = compactMrzText(ocrText);
        List<String> candidates = new ArrayList<>();

        Matcher matcher = MRZ_LINE_2_CANDIDATE.matcher(compact);
        while (matcher.find()) {
            candidates.add(matcher.group());
        }

        String coreLine2 = buildLine2FromBgdCore(compact);
        if (coreLine2 != null) {
            candidates.add(0, coreLine2);
        }

        for (String line2 : candidates) {
            String repaired = repairMrzLine2(line2);
            if (!passesMrzLine2Checksums(repaired)) {
                continue;
            }
            String line1 = findMatchingMrzLine1(compact, repaired);
            if (line1 == null) {
                line1 = findMrzLine1ByPattern(compact);
            }
            if (line1 != null) {
                return new ValidMrz(normalizeMrzLine1(line1), repaired);
            }
        }
        return null;
    }

    private String compactMrzText(String ocrText) {
        return ocrText.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9<\\n]", "").replace("\n", "");
    }

    private String buildLine2FromBgdCore(String compact) {
        Matcher matcher = MRZ_LINE_2_BGD_CORE.matcher(compact);
        if (!matcher.find()) {
            return null;
        }
        int start = matcher.start();
        int end = Math.min(compact.length(), start + MRZ_LINE_LENGTH);
        String line = compact.substring(start, end);
        if (line.length() < MRZ_LINE_LENGTH) {
            line = line + "<".repeat(MRZ_LINE_LENGTH - line.length());
        }
        return line.substring(0, MRZ_LINE_LENGTH);
    }

    private ExtractedPassportFields extractLooseMrzFields(String ocrText) {
        String compact = compactMrzText(ocrText);
        String line2 = buildLine2FromBgdCore(compact);
        if (line2 != null) {
            line2 = repairMrzLine2(line2);
        }

        String line1 = findMrzLine1ByPattern(compact);
        if (line1 != null) {
            line1 = normalizeMrzLine1(line1);
        }

        if (line2 != null && passesMrzLine2Checksums(line2)) {
            ExtractedPassportFields parsed = parseMrz(
                    line1 != null ? line1 : "P<BGD<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
                    line2);
            return enrichWithLooseMrzNames(parsed, compact, line2);
        }

        String passportNumber = findEmbeddedPassportNumber(compact);
        String surname = null;
        String givenNames = null;
        String issuingCountry = "BGD";

        Matcher line1Matcher = MRZ_LINE_1_BGD.matcher(compact);
        if (line1Matcher.find()) {
            surname = cleanPersonName(trimTrailingMrzNoise(line1Matcher.group(1)));
            givenNames = cleanPersonName(line1Matcher.group(2).replace('<', ' '));
            issuingCountry = "BGD";
        }
        if (surname == null || givenNames == null) {
            String[] looseNames = findLooseMrzNames(compact, line2);
            if (looseNames != null) {
                surname = firstNonNull(surname, looseNames[0]);
                givenNames = firstNonNull(givenNames, looseNames[1]);
            }
        }

        String nationality = null;
        String dateOfBirth = null;
        String dateOfExpiry = null;
        String gender = null;

        if (line2 != null && line2.length() >= 27) {
            passportNumber = firstNonNull(passportNumber, cleanMrzField(line2.substring(0, 9)));
            nationality = cleanMrzField(line2.substring(10, 13));
            dateOfBirth = cleanMrzField(line2.substring(13, 19));
            gender = normalizeGender(cleanMrzField(line2.substring(20, 21)));
            dateOfExpiry = cleanMrzField(line2.substring(21, 27));
        }

        return new ExtractedPassportFields(
                surname,
                givenNames,
                passportNumber,
                nationality,
                dateOfBirth,
                dateOfExpiry,
                null,
                gender,
                null,
                issuingCountry,
                line1,
                line2
        );
    }

    private String findMrzLine1ByPattern(String compact) {
        Matcher matcher = MRZ_LINE_1_BGD.matcher(compact);
        if (!matcher.find()) {
            return null;
        }
        String surname = matcher.group(1);
        String given = matcher.group(2).replaceAll("<+$", "");
        return normalizeMrzLine1("P<BGD" + surname + "<<" + given);
    }

    private ExtractedPassportFields enrichWithLooseMrzNames(
            ExtractedPassportFields parsed, String compact, String line2) {
        if (parsed.surname() != null && parsed.givenNames() != null) {
            return parsed;
        }
        String[] looseNames = findLooseMrzNames(compact, line2);
        if (looseNames == null) {
            return parsed;
        }
        String surname = firstNonNull(parsed.surname(), looseNames[0]);
        String givenNames = parsed.givenNames();
        if (givenNames == null && looseNames[1] != null
                && !mrzDefinitivelySingleName(parsed.mrzLine1())
                && isPlausibleGivenName(looseNames[1], surname)) {
            givenNames = looseNames[1];
        }
        String line1 = parsed.mrzLine1();
        if (line1 == null || line1.startsWith("P<BGD<")) {
            line1 = buildMrzLine1FromNames(surname, givenNames);
        }
        return new ExtractedPassportFields(
                surname,
                givenNames,
                parsed.passportNumber(),
                parsed.nationality(),
                parsed.dateOfBirth(),
                parsed.dateOfExpiry(),
                parsed.dateOfIssue(),
                parsed.gender(),
                parsed.placeOfBirth(),
                parsed.issuingCountry(),
                line1,
                parsed.mrzLine2());
    }

    private String buildMrzLine1FromNames(String surname, String givenNames) {
        if (surname == null) {
            return null;
        }
        String givenPart = givenNames == null ? "" : givenNames.trim().replace(' ', '<');
        return normalizeMrzLine1("P<BGD" + surname + "<<" + givenPart);
    }

    private String[] findLooseMrzNames(String compact, String anchor) {
        if (compact == null || compact.isBlank()) {
            return null;
        }
        int end = compact.length();
        if (anchor != null && anchor.length() >= 9) {
            String passportPrefix = anchor.substring(0, 9);
            int idx = compact.lastIndexOf(passportPrefix);
            if (idx > 0) {
                end = idx;
            }
        }
        int start = Math.max(0, end - 200);
        String window = compact.substring(start, end);

        Matcher matcher = MRZ_LOOSE_NAME_PATTERN.matcher(window);
        String bestSurname = null;
        String bestGiven = null;
        int bestStart = -1;
        int bestGroupLength = Integer.MAX_VALUE;
        while (matcher.find()) {
            String surnameCandidate = extractLooseSurname(matcher.group(1));
            String givenCandidate = extractLooseGivenName(matcher.group(2));
            if (surnameCandidate == null || givenCandidate == null) {
                continue;
            }
            if (isMrzNameNoise(surnameCandidate) || isMrzNameNoise(givenCandidate)) {
                continue;
            }
            int groupLength = matcher.group(1).length();
            if (groupLength < bestGroupLength
                    || (groupLength == bestGroupLength && matcher.start() > bestStart)) {
                bestStart = matcher.start();
                bestGroupLength = groupLength;
                bestSurname = surnameCandidate;
                bestGiven = givenCandidate;
            }
        }
        if (bestSurname == null) {
            return null;
        }
        return new String[]{
                cleanPersonName(trimTrailingMrzNoise(bestSurname)),
                cleanPersonName(bestGiven)
        };
    }

    private String extractLooseSurname(String rawGroup) {
        if (rawGroup == null || rawGroup.isBlank()) {
            return null;
        }
        String upper = rawGroup.toUpperCase(Locale.ROOT);
        if (upper.startsWith("BGD") && upper.length() > 3) {
            upper = upper.substring(3);
        }
        for (int len = Math.min(8, upper.length()); len >= 4; len--) {
            String suffix = upper.substring(upper.length() - len);
            if (suffix.matches("[A-Z]+") && !isMrzNameNoise(suffix)) {
                return suffix;
            }
        }
        return upper.length() >= 3 && upper.matches("[A-Z]+") && !isMrzNameNoise(upper) ? upper : null;
    }

    private String extractLooseGivenName(String rawGroup) {
        if (rawGroup == null || rawGroup.isBlank()) {
            return null;
        }
        String token = rawGroup.toUpperCase(Locale.ROOT).replaceAll("[<KE].*", "").trim();
        return token.length() >= 3 ? token : null;
    }

    private boolean isMrzNameNoise(String token) {
        if (token == null || token.length() < 3) {
            return true;
        }
        String upper = token.toUpperCase(Locale.ROOT);
        if (MRZ_NAME_NOISE.contains(upper)) {
            return true;
        }
        return upper.contains("PASSPORT") || upper.contains("BANGLADESHI") || upper.contains("REPUBLIC");
    }

    private String normalizeMrzLine1(String line1) {
        if (line1 == null) {
            return null;
        }
        String compact = line1.replace(" ", "").toUpperCase(Locale.ROOT);
        if (compact.length() >= MRZ_LINE_LENGTH) {
            return compact.substring(0, MRZ_LINE_LENGTH);
        }
        return compact + "<".repeat(MRZ_LINE_LENGTH - compact.length());
    }

    private String repairMrzLine2(String line) {
        if (line == null || line.length() != MRZ_LINE_LENGTH) {
            return line;
        }
        if (passesMrzLine2Checksums(line) && isValidMrzSex(line.charAt(20))) {
            return line;
        }

        char sex = line.charAt(20);
        if (!isValidMrzSex(sex)) {
            for (char candidate : new char[]{'M', 'F'}) {
                String repaired = line.substring(0, 20) + candidate + line.substring(21);
                if (passesMrzLine2Checksums(repaired)) {
                    return repaired;
                }
            }
        }

        if (passesMrzLine2Checksums(line)) {
            char inferred = inferSexFromLine2(line);
            return line.substring(0, 20) + inferred + line.substring(21);
        }
        return line;
    }

    private char inferSexFromLine2(String line) {
        if (line.length() > 20) {
            char raw = line.charAt(20);
            if (raw == '4' || raw == 'W' || raw == 'H') {
                return 'M';
            }
            if (raw == 'E' || raw == 'P') {
                return 'F';
            }
        }
        return 'M';
    }

    private boolean passesMrzLine2Checksums(String line) {
        if (line == null || line.length() != MRZ_LINE_LENGTH) {
            return false;
        }
        if (!line.chars().allMatch(ch -> Character.isUpperCase(ch) || Character.isDigit(ch) || ch == '<')) {
            return false;
        }
        return mrzCheckDigit(line.substring(0, 9), line.charAt(9))
                && mrzCheckDigit(line.substring(13, 19), line.charAt(19))
                && mrzCheckDigit(line.substring(21, 27), line.charAt(27));
    }

    private boolean isValidMrzSex(char sex) {
        return sex == 'M' || sex == 'F' || sex == '<';
    }

    private String findMatchingMrzLine1(String compact, String line2) {
        int idx = compact.indexOf(line2);
        if (idx < 0) {
            return null;
        }
        String before = compact.substring(0, idx).replace("\n", "");
        for (int start = Math.max(0, before.length() - 120); start <= before.length() - MRZ_LINE_LENGTH; start++) {
            String candidate = before.substring(start, start + MRZ_LINE_LENGTH);
            if (candidate.startsWith("P<") && isValidMrzLine1(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isValidMrzLine1(String line) {
        if (line == null || line.length() != MRZ_LINE_LENGTH) {
            return false;
        }
        return line.startsWith("P<") && line.chars().allMatch(ch -> Character.isUpperCase(ch) || Character.isDigit(ch) || ch == '<');
    }

    private boolean isValidMrzLine2(String line) {
        return passesMrzLine2Checksums(line) && isValidMrzSex(line.charAt(20));
    }

    private boolean mrzCheckDigit(String data, char expected) {
        if (expected == '<') {
            return true;
        }
        int[] weights = {7, 3, 1};
        int sum = 0;
        for (int i = 0; i < data.length(); i++) {
            int value = mrzCharValue(data.charAt(i));
            if (value < 0) {
                return false;
            }
            sum += value * weights[i % 3];
        }
        int check = sum % 10;
        return Character.digit(expected, 10) == check;
    }

    private int mrzCharValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        }
        if (c == '<') {
            return 0;
        }
        return -1;
    }

    private ExtractedPassportFields parseMrz(String line1, String line2) {
        String issuingCountry = cleanValue(line1.substring(2, 5));
        String namesPart = line1.substring(5);
        String[] nameParts = namesPart.split("<<", 2);
        String surname = nameParts.length > 0 ? repairCommonNameOcrErrors(cleanPersonName(nameParts[0])) : null;
        String givenNames = extractGivenNamesFromMrzPart(nameParts.length > 1 ? nameParts[1] : null);

        String passportNumber = cleanMrzField(line2.substring(0, 9));
        String nationality = cleanMrzField(line2.substring(10, 13));
        String dateOfBirth = cleanMrzField(line2.substring(13, 19));
        String gender = normalizeGender(cleanMrzField(line2.substring(20, 21)));
        String dateOfExpiry = cleanMrzField(line2.substring(21, 27));

        return new ExtractedPassportFields(
                surname,
                givenNames,
                passportNumber,
                nationality,
                dateOfBirth,
                dateOfExpiry,
                null,
                gender,
                null,
                issuingCountry,
                line1,
                line2
        );
    }

    private ExtractedPassportFields extractVisualFields(String rawOcr) {
        String normalized = normalizeOcr(rawOcr);
        List<String> lines = normalized.lines()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        String upper = normalized.toUpperCase(Locale.ROOT);

        String surname = cleanPersonName(findLabeledValue(lines, SURNAME_PATTERN));
        String givenNames = cleanPersonName(findLabeledValue(lines, GIVEN_NAME_PATTERN));
        givenNames = sanitizeVisualGivenNames(givenNames, surname);
        String nationality = findLabeledValue(lines, NATIONALITY_PATTERN);
        String placeOfBirth = findLabeledValue(lines, PLACE_OF_BIRTH_PATTERN);
        String gender = firstMatch(upper, SEX_PATTERN);
        if (gender == null) {
            gender = firstMatch(upper, LOOSE_SEX_PATTERN);
        }

        String passportNumber = null;
        Matcher passportMatcher = PASSPORT_NUMBER_LABEL_PATTERN.matcher(upper);
        if (passportMatcher.find()) {
            passportNumber = cleanValue(passportMatcher.group(1));
        }
        if (passportNumber == null) {
            passportNumber = findEmbeddedPassportNumber(compactMrzText(rawOcr));
        }
        if (passportNumber == null) {
            passportNumber = findBestPassportNumber(upper);
        }

        if (surname == null || givenNames == null) {
            Matcher mrzNameMatcher = MRZ_LINE_1_BGD.matcher(compactMrzText(rawOcr));
            if (mrzNameMatcher.find()) {
                if (surname == null) {
                    surname = cleanPersonName(trimTrailingMrzNoise(mrzNameMatcher.group(1)));
                }
                if (givenNames == null) {
                    givenNames = extractGivenNamesFromMrzPart(mrzNameMatcher.group(2).replace('<', ' '));
                }
            }
        }
        if (surname == null || givenNames == null) {
            String compact = compactMrzText(rawOcr);
            String anchor = passportNumber != null ? passportNumber : findEmbeddedPassportNumber(compact);
            String[] looseNames = findLooseMrzNames(compact, anchor);
            if (looseNames != null) {
                surname = firstNonNull(surname, looseNames[0]);
                if (givenNames == null && looseNames[1] != null && isPlausibleGivenName(looseNames[1], surname)) {
                    givenNames = looseNames[1];
                }
            }
        }

        String dateOfBirth = null;
        String dateOfExpiry = null;
        String dateOfIssue = null;
        for (String line : lines) {
            Matcher matcher = DATE_WITH_LABEL_PATTERN.matcher(line.toUpperCase(Locale.ROOT));
            if (!matcher.find()) {
                continue;
            }
            String label = matcher.group(1).toLowerCase(Locale.ROOT);
            String value = cleanValue(matcher.group(2));
            if (label.contains("birth") || label.contains("dob")) {
                dateOfBirth = value;
            } else if (label.contains("expiry")) {
                dateOfExpiry = value;
            } else if (label.contains("issue")) {
                dateOfIssue = value;
            }
        }

        List<String> standaloneDates = new ArrayList<>();
        Matcher dateMatcher = STANDALONE_DATE_PATTERN.matcher(upper);
        while (dateMatcher.find()) {
            standaloneDates.add(cleanValue(dateMatcher.group(1)));
        }
        String compactUpper = compactMrzText(rawOcr);
        Matcher compactDateMatcher = COMPACT_ALPHA_DATE_PATTERN.matcher(compactUpper);
        while (compactDateMatcher.find()) {
            standaloneDates.add(cleanValue(compactDateMatcher.group(1) + " "
                    + compactDateMatcher.group(2) + " " + compactDateMatcher.group(3)));
        }
        Matcher spacedCompactDateMatcher = COMPACT_ALPHA_DATE_PATTERN.matcher(upper.replaceAll("\\s+", ""));
        while (spacedCompactDateMatcher.find()) {
            standaloneDates.add(cleanValue(spacedCompactDateMatcher.group(1) + " "
                    + spacedCompactDateMatcher.group(2) + " " + spacedCompactDateMatcher.group(3)));
        }
        if (dateOfBirth == null) {
            dateOfBirth = pickStandaloneDateByKind(standaloneDates, DateKind.BIRTH);
        }
        if (dateOfIssue == null) {
            dateOfIssue = pickStandaloneDateByKind(standaloneDates, DateKind.ISSUE);
        }
        if (dateOfExpiry == null) {
            dateOfExpiry = pickStandaloneDateByKind(standaloneDates, DateKind.EXPIRY);
        }

        if (nationality != null && nationality.toUpperCase(Locale.ROOT).contains("BANGLADESHI")) {
            nationality = "BGD";
        }

        return new ExtractedPassportFields(
                surname,
                givenNames,
                passportNumber,
                nationality,
                dateOfBirth,
                dateOfExpiry,
                dateOfIssue,
                gender,
                placeOfBirth,
                "BGD",
                null,
                null
        );
    }

    private ExtractedPassportFields mergeFields(ExtractedPassportFields visual, ExtractedPassportFields mrz) {
        return new ExtractedPassportFields(
                mergeSurname(visual.surname(), mrz.surname()),
                mergeGivenNames(visual, mrz),
                firstNonNull(mrz.passportNumber(), visual.passportNumber()),
                firstNonNull(mrz.nationality(), visual.nationality()),
                preferReadableDate(visual.dateOfBirth(), mrz.dateOfBirth()),
                preferReadableDate(visual.dateOfExpiry(), mrz.dateOfExpiry()),
                firstNonNull(visual.dateOfIssue(), inferIssueDateFromExpiry(mrz.dateOfExpiry())),
                firstNonNull(mrz.gender(), visual.gender()),
                visual.placeOfBirth(),
                firstNonNull(mrz.issuingCountry(), visual.issuingCountry()),
                mrz.mrzLine1(),
                mrz.mrzLine2()
        );
    }

    private String pickStandaloneDateByKind(List<String> dates, DateKind dateKind) {
        for (String date : dates) {
            String iso = normalizeDateToIso(date, dateKind);
            if (iso == null) {
                continue;
            }
            int year = Integer.parseInt(iso.substring(0, 4));
            boolean matches = switch (dateKind) {
                case BIRTH -> year >= 1940 && year <= 2015;
                case ISSUE -> year >= 2010 && year <= 2030;
                case EXPIRY -> year >= 2031 && year <= 2050;
            };
            if (matches) {
                return date;
            }
        }
        return null;
    }

    private String mergeSurname(String visualSurname, String mrzSurname) {
        String repairedMrz = repairCommonNameOcrErrors(mrzSurname);
        if (mrzSurname == null) {
            return visualSurname;
        }
        if (visualSurname == null || mrzSurname.equals(visualSurname)) {
            return repairedMrz;
        }
        if (isSimilarName(mrzSurname, visualSurname) || isSimilarName(repairedMrz, visualSurname)) {
            return visualSurname.length() >= repairedMrz.length() ? visualSurname : repairedMrz;
        }
        return repairedMrz;
    }

    private String mergeGivenNames(ExtractedPassportFields visual, ExtractedPassportFields mrz) {
        if (mrz.givenNames() != null) {
            return mrz.givenNames();
        }
        if (mrzDefinitivelySingleName(mrz.mrzLine1())) {
            return null;
        }
        String visualGiven = visual.givenNames();
        if (visualGiven != null && isPlausibleGivenName(visualGiven, mrz.surname(), visual.surname())) {
            return visualGiven;
        }
        return null;
    }

    private boolean mrzDefinitivelySingleName(String line1) {
        if (line1 == null || line1.length() <= 5) {
            return false;
        }
        String namesPart = line1.substring(5);
        if (!namesPart.chars().anyMatch(Character::isLetter)) {
            return false;
        }
        int separator = namesPart.indexOf("<<");
        if (separator < 0) {
            return false;
        }
        String givenPart = namesPart.substring(separator + 2);
        return !givenPart.chars().anyMatch(Character::isLetter);
    }

    private String extractGivenNamesFromMrzPart(String rawGivenPart) {
        if (rawGivenPart == null) {
            return null;
        }
        String lettersOnly = rawGivenPart.replace('<', ' ').replaceAll("[^A-Za-z]", "").trim();
        if (lettersOnly.isBlank()) {
            return null;
        }
        return cleanPersonName(rawGivenPart.replace('<', ' '));
    }

    private boolean mrzHasGivenNames(String line1) {
        return !mrzDefinitivelySingleName(line1);
    }

    private String repairCommonNameOcrErrors(String name) {
        if (name == null) {
            return null;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.endsWith("GLR") && upper.length() >= 5) {
            return upper.substring(0, upper.length() - 3) + "GIR";
        }
        return upper;
    }

    private String sanitizeVisualGivenNames(String givenNames, String surname) {
        if (givenNames == null) {
            return null;
        }
        return isPlausibleGivenName(givenNames, surname) ? givenNames : null;
    }

    private boolean isPlausibleGivenName(String givenNames, String... surnames) {
        if (givenNames == null || givenNames.isBlank()) {
            return false;
        }
        String upper = givenNames.toUpperCase(Locale.ROOT).trim();
        if (GIVEN_NAME_NOISE.contains(upper)) {
            return false;
        }
        if (isMrzNameNoise(upper) || isLikelyFieldLabel(upper)) {
            return false;
        }
        for (String surname : surnames) {
            if (surname != null && upper.equals(surname.toUpperCase(Locale.ROOT))) {
                return false;
            }
        }
        return upper.length() >= 3;
    }

    private boolean isSimilarName(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        String a = left.toUpperCase(Locale.ROOT);
        String b = right.toUpperCase(Locale.ROOT);
        if (a.equals(b)) {
            return true;
        }
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0 || Math.abs(a.length() - b.length()) > 2) {
            return false;
        }
        return levenshteinDistance(a, b) <= 2;
    }

    private int levenshteinDistance(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[left.length()][right.length()];
    }

    private String inferIssueDateFromExpiry(String expiryDate) {
        String expiryIso = normalizeDateToIso(expiryDate, DateKind.EXPIRY);
        if (expiryIso == null) {
            return null;
        }
        try {
            return LocalDate.parse(expiryIso).minusYears(10).plusDays(1).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String preferReadableDate(String visualDate, String mrzDate) {
        if (mrzDate != null && mrzDate.matches("\\d{6}")) {
            return mrzDate;
        }
        if (visualDate != null && !visualDate.matches("\\d{6}")) {
            return visualDate;
        }
        return mrzDate != null ? mrzDate : visualDate;
    }

    private String findEmbeddedPassportNumber(String compact) {
        Matcher matcher = MRZ_EMBEDDED_PASSPORT_PATTERN.matcher(compact);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String findBestPassportNumber(String upperText) {
        Matcher matcher = BGD_PASSPORT_NUMBER_PATTERN.matcher(upperText);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (isValidBangladeshPassportNumber(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String trimTrailingMrzNoise(String surname) {
        if (surname == null || surname.length() <= 4) {
            return surname;
        }
        if (surname.endsWith("K") || surname.endsWith("KK")) {
            return surname.replaceAll("K+$", "");
        }
        return surname;
    }

    private String normalizeGender(String gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender.toUpperCase(Locale.ROOT)) {
            case "M", "F", "X" -> gender.toUpperCase(Locale.ROOT);
            case "4", "W", "H" -> "M";
            case "E", "P" -> "F";
            default -> null;
        };
    }

    private String findLabeledValue(List<String> lines, Pattern pattern) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = pattern.matcher(lines.get(i));
            if (!matcher.find()) {
                continue;
            }
            String value = cleanValue(matcher.group(1));
            if (value != null && !isLikelyFieldLabel(value)) {
                return value;
            }
            if (i + 1 < lines.size()) {
                String next = cleanValue(lines.get(i + 1));
                if (next != null && !isLikelyFieldLabel(next)) {
                    return next;
                }
            }
        }
        return null;
    }

    private boolean isLikelyFieldLabel(String value) {
        if (value == null) {
            return true;
        }
        String v = value.toLowerCase(Locale.ROOT);
        return v.contains("surname")
                || v.contains("given name")
                || v.contains("nationality")
                || v.contains("place of birth")
                || v.contains("date of")
                || v.equals("name")
                || v.equals("war")
                || v.equals("nam");
    }

    private String firstMatch(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return cleanValue(matcher.group(matcher.groupCount()));
    }

    private String normalizeOcr(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('\r', '\n')
                .replaceAll("[\\u0000-\\u001f&&[^\\n\\t]]", " ")
                .replaceAll("[ ]{2,}", " ")
                .trim();
    }

    private boolean hasMeaningfulExtraction(ExtractedPassportFields fields) {
        if (isValidBangladeshPassportNumber(fields.passportNumber())) {
            return true;
        }
        if (fields.mrzLine2() != null && passesMrzLine2Checksums(fields.mrzLine2())) {
            return true;
        }
        return fields.surname() != null
                && fields.dateOfBirth() != null
                && (fields.passportNumber() != null || fields.givenNames() != null);
    }

    private boolean isValidBangladeshPassportNumber(String value) {
        return value != null && value.matches("[A-Z][0-9]{8}");
    }

    private String cleanMrzField(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace('<', ' ').trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String cleanPersonName(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.toUpperCase(Locale.ROOT)
                .replace('<', ' ')
                .replaceAll("[^A-Z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isBlank()) {
            return null;
        }

        String[] parts = cleaned.split("\\s+");
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (isMrzFillerToken(part)) {
                break;
            }
            part = trimTrailingOcrNoiseFromToken(part);
            part = stripLeadingBgdPrefix(part);
            part = repairCommonNameOcrErrors(part);
            if (part == null || part.isBlank()) {
                continue;
            }
            if (part.length() <= 1 && !part.equals("M") && !part.equals("F")) {
                continue;
            }
            kept.add(part);
        }
        return kept.isEmpty() ? null : String.join(" ", kept);
    }

    private boolean isMrzFillerToken(String token) {
        if (token == null || token.isBlank()) {
            return true;
        }
        if (token.matches(".*([KX])\\1{2,}.*")) {
            return true;
        }
        if (token.length() >= 4 && token.chars().distinct().count() <= 2) {
            return true;
        }
        long fillerChars = token.chars().filter(ch -> ch == 'K' || ch == 'X' || ch == 'E').count();
        return token.length() >= 4 && fillerChars >= token.length() - 1;
    }

    private String stripLeadingBgdPrefix(String token) {
        if (token != null && token.startsWith("BGD") && token.length() > 3) {
            return token.substring(3);
        }
        return token;
    }

    private String trimTrailingOcrNoiseFromToken(String token) {
        if (token.matches(".*[KX]{2,}.*")) {
            return token.replaceAll("[KXES]+$", "");
        }
        if (token.length() > 6 && token.endsWith("S")) {
            return token.substring(0, token.length() - 1);
        }
        if (token.length() > 4 && token.endsWith("X")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String firstNonNull(String primary, String fallback) {
        return primary != null ? primary : fallback;
    }

    private String normalizeDateToIso(String rawValue, DateKind dateKind) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String value = rawValue.trim().toUpperCase(Locale.ROOT);

        Matcher compactDate = COMPACT_ALPHA_DATE_PATTERN.matcher(value.replaceAll("\\s+", ""));
        if (compactDate.find()) {
            value = compactDate.group(1) + " " + compactDate.group(2) + " " + compactDate.group(3);
        }

        if (value.matches("\\d{6}")) {
            int yy = Integer.parseInt(value.substring(0, 2));
            int month = Integer.parseInt(value.substring(2, 4));
            int day = Integer.parseInt(value.substring(4, 6));
            int year = resolveYearFromTwoDigits(yy, dateKind);
            return safeIso(year, month, day);
        }

        String compactNumeric = value.replace('.', '/').replace('-', '/');
        for (DateTimeFormatter formatter : Arrays.asList(
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uu"),
                DateTimeFormatter.ofPattern("uuuu/MM/dd"))) {
            try {
                LocalDate parsed = LocalDate.parse(compactNumeric, formatter);
                if (formatter.toString().contains("uu") && !formatter.toString().contains("uuuu")) {
                    parsed = parsed.withYear(resolveYearFromTwoDigits(parsed.getYear() % 100, dateKind));
                }
                return parsed.toString();
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        String normalizedAlpha = value.replaceAll("\\s+", " ");
        for (String candidate : visualDateCandidates(normalizedAlpha)) {
            for (DateTimeFormatter formatter : Arrays.asList(
                    VISUAL_DATE_FORMATTER,
                    VISUAL_DATE_SHORT_YEAR_FORMATTER)) {
                try {
                    LocalDate parsed = LocalDate.parse(candidate, formatter);
                    if (formatter == VISUAL_DATE_SHORT_YEAR_FORMATTER) {
                        parsed = parsed.withYear(resolveYearFromTwoDigits(parsed.getYear() % 100, dateKind));
                    }
                    return parsed.toString();
                } catch (DateTimeParseException ignored) {
                    // Try next format
                }
            }
        }

        return null;
    }

    private List<String> visualDateCandidates(String normalizedAlpha) {
        List<String> candidates = new ArrayList<>();
        candidates.add(normalizedAlpha);

        Matcher matcher = Pattern.compile("^(\\d{2})\\s+([A-Z]{3})\\s+(\\d{2,4})$").matcher(normalizedAlpha);
        if (!matcher.matches()) {
            return candidates;
        }

        String dayPart = matcher.group(1);
        String monthPart = matcher.group(2);
        String yearPart = matcher.group(3).replace('O', '0');

        int day = Integer.parseInt(dayPart);
        if (day > 31 && dayPart.charAt(0) == '4') {
            candidates.add("1" + dayPart.substring(1) + " " + monthPart + " " + yearPart);
        }
        if (!yearPart.equals(matcher.group(3))) {
            candidates.add(dayPart + " " + monthPart + " " + yearPart);
        }
        return candidates;
    }

    private int resolveYearFromTwoDigits(int twoDigitYear, DateKind dateKind) {
        int currentYear = Year.now().getValue();
        int currentCentury = (currentYear / 100) * 100;
        int candidate = currentCentury + twoDigitYear;

        return switch (dateKind) {
            case BIRTH -> candidate > currentYear ? candidate - 100 : candidate;
            case EXPIRY, ISSUE -> candidate < (currentYear - 50) ? candidate + 100 : candidate;
        };
    }

    private String safeIso(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private enum DateKind {
        BIRTH,
        EXPIRY,
        ISSUE
    }

    private record ValidMrz(String line1, String line2) {}

    private record ExtractedPassportFields(
            String surname,
            String givenNames,
            String passportNumber,
            String nationality,
            String dateOfBirth,
            String dateOfExpiry,
            String dateOfIssue,
            String gender,
            String placeOfBirth,
            String issuingCountry,
            String mrzLine1,
            String mrzLine2) {}

    private void validatePassportImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Passport image file cannot be empty");
        }

        String ct = file.getContentType();
        if (ct == null || (!ct.equals("image/jpeg") && !ct.equals("image/jpg") && !ct.equals("image/png"))) {
            throw new IllegalArgumentException("Passport image must be JPEG or PNG. Got: " + ct);
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Passport image too large. Maximum size is 10 MB. Got: " + file.getSize() + " bytes");
        }

        log.debug("Image validation passed - Type: {}, Size: {} bytes", ct, file.getSize());
    }
}
