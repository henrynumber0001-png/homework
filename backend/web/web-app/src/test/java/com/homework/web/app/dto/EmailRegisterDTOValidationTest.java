package com.homework.web.app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailRegisterDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsPasswordAtMinimumLength() {
        EmailRegisterDTO dto = validDto();
        dto.setPassword("12345678");
        dto.setPasswordConfirm("12345678");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void acceptsPasswordAtMaximumLength() {
        EmailRegisterDTO dto = validDto();
        dto.setPassword("1234567890123456");
        dto.setPasswordConfirm("1234567890123456");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        EmailRegisterDTO dto = validDto();
        dto.setPassword("1234567");

        assertHasViolation(validator.validate(dto), "password");
    }

    @Test
    void rejectsPasswordLongerThanSixteenCharacters() {
        EmailRegisterDTO dto = validDto();
        dto.setPassword("12345678901234567");

        assertHasViolation(validator.validate(dto), "password");
    }

    @Test
    void rejectsInvalidEmail() {
        EmailRegisterDTO dto = validDto();
        dto.setEmail("not-an-email");

        assertHasViolation(validator.validate(dto), "email");
    }

    @Test
    void rejectsBlankRequiredFields() {
        EmailRegisterDTO dto = new EmailRegisterDTO();

        Set<ConstraintViolation<EmailRegisterDTO>> violations = validator.validate(dto);

        assertHasViolation(violations, "email");
        assertHasViolation(violations, "password");
        assertHasViolation(violations, "passwordConfirm");
        assertHasViolation(violations, "secureTicket");
        assertHasViolation(violations, "displayName");
    }

    @Test
    void rejectsDisplayNameLongerThanTwentyCharacters() {
        EmailRegisterDTO dto = validDto();
        dto.setDisplayName("a".repeat(21));

        assertHasViolation(validator.validate(dto), "displayName");
    }

    private static EmailRegisterDTO validDto() {
        EmailRegisterDTO dto = new EmailRegisterDTO();
        dto.setEmail("user@example.com");
        dto.setPassword("password123");
        dto.setPasswordConfirm("password123");
        dto.setSecureTicket("secure-ticket");
        dto.setDisplayName("Henry");
        return dto;
    }

    private static void assertHasViolation(
            Set<ConstraintViolation<EmailRegisterDTO>> violations,
            String propertyName) {
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(violation ->
                propertyName.equals(violation.getPropertyPath().toString())));
    }
}
