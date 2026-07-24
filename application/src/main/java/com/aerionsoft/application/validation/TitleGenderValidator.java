package com.aerionsoft.application.validation;

import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.enums.user.Gender;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;

public class TitleGenderValidator implements ConstraintValidator<ValidTitleGender, TravellerRequest> {

    private static final Map<String, Gender> TITLE_GENDER_MAP = Map.of(
            "MR", Gender.MALE,
            "MSTR", Gender.MALE,
            "MISS", Gender.FEMALE,
            "MRS", Gender.FEMALE,
            "MS", Gender.FEMALE
    );

    @Override
    public boolean isValid(TravellerRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getTitle() == null) {
            return true;
        }

        String normalizedTitle = normalizeTitle(request.getTitle());
        Gender requiredGender = TITLE_GENDER_MAP.get(normalizedTitle);
        if (requiredGender == null) {
            return true;
        }

        if (request.getGender() == requiredGender) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(buildMessage(normalizedTitle, requiredGender))
                .addPropertyNode("gender")
                .addConstraintViolation();
        return false;
    }

    private static String normalizeTitle(String title) {
        return title.trim()
                .replace(".", "")
                .replaceAll("\\s+", "")
                .toUpperCase();
    }

    private static String buildMessage(String normalizedTitle, Gender requiredGender) {
        String titleLabel = switch (normalizedTitle) {
            case "MR" -> "Mr";
            case "MSTR" -> "Mstr";
            case "MISS" -> "Miss";
            case "MRS" -> "Mrs";
            case "MS" -> "Ms";
            default -> normalizedTitle;
        };
        return "When title is " + titleLabel + ", gender must be " + requiredGender.getDisplay();
    }
}
