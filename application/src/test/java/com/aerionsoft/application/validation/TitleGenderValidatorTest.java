package com.aerionsoft.application.validation;

import com.aerionsoft.application.dto.traveller.TravellerRequest;
import com.aerionsoft.application.enums.user.Gender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TitleGenderValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void mrTitleWithMaleGenderIsValid() {
        TravellerRequest request = validTraveller();
        request.setTitle("Mr");
        request.setGender(Gender.MALE);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void mstrTitleWithMaleGenderIsValid() {
        TravellerRequest request = validTraveller();
        request.setTitle("Mstr.");
        request.setGender(Gender.MALE);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void missTitleWithFemaleGenderIsValid() {
        TravellerRequest request = validTraveller();
        request.setTitle("Miss");
        request.setGender(Gender.FEMALE);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void mrTitleWithFemaleGenderIsInvalid() {
        TravellerRequest request = validTraveller();
        request.setTitle("MR.");
        request.setGender(Gender.FEMALE);

        Set<ConstraintViolation<TravellerRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("When title is Mr, gender must be Male");
    }

    @Test
    void mstrTitleWithFemaleGenderIsInvalid() {
        TravellerRequest request = validTraveller();
        request.setTitle("MSTR");
        request.setGender(Gender.FEMALE);

        Set<ConstraintViolation<TravellerRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("When title is Mstr, gender must be Male");
    }

    @Test
    void missTitleWithMaleGenderIsInvalid() {
        TravellerRequest request = validTraveller();
        request.setTitle("Miss");
        request.setGender(Gender.MALE);

        Set<ConstraintViolation<TravellerRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("When title is Miss, gender must be Female");
    }

    @Test
    void mrsTitleWithFemaleGenderIsValid() {
        TravellerRequest request = validTraveller();
        request.setTitle("Mrs.");
        request.setGender(Gender.FEMALE);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void mrsTitleWithMaleGenderIsInvalid() {
        TravellerRequest request = validTraveller();
        request.setTitle("MRS");
        request.setGender(Gender.MALE);

        Set<ConstraintViolation<TravellerRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("When title is Mrs, gender must be Female");
    }

    @Test
    void msTitleWithFemaleGenderIsValid() {
        TravellerRequest request = validTraveller();
        request.setTitle("Ms.");
        request.setGender(Gender.FEMALE);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void msTitleWithMaleGenderIsInvalid() {
        TravellerRequest request = validTraveller();
        request.setTitle("MS");
        request.setGender(Gender.MALE);

        Set<ConstraintViolation<TravellerRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("When title is Ms, gender must be Female");
    }

    @Test
    void otherTitleDoesNotEnforceGenderMatch() {
        TravellerRequest request = validTraveller();
        request.setTitle("Dr");
        request.setGender(Gender.MALE);

        assertThat(validator.validate(request)).isEmpty();
    }

    private static TravellerRequest validTraveller() {
        TravellerRequest request = new TravellerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setMobileCountryCode("+971");
        request.setDob("1990-01-01");
        request.setNationality("AE");
        return request;
    }
}
