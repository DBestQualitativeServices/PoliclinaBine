package com.example.policlicabine.service;

import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormSection;
import com.example.policlicabine.model.FormStructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive validation tests for FormValidationService.
 * Tests all validation rules including required fields, patterns, ranges, and edge cases.
 */
class FormValidationServiceTest {

    private FormValidationService formValidationService;
    private FormStructure testStructure;
    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        formValidationService = new FormValidationService();
        testStructure = buildFormStructure();
        testData = new HashMap<>();
    }

    // ==================== REQUIRED FIELD TESTS ====================

    @Test
    void shouldFailWhenRequiredFieldMissing() {
        // Arrange
        testData.put("optionalField", "value");
        // Missing required 'email' field

        // Act
        List<String> errors = formValidationService.validate(testStructure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
    }

    @Test
    void shouldFailWhenRequiredFieldNull() {
        // Arrange
        testData.put("email", null);

        // Act
        List<String> errors = formValidationService.validate(testStructure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
    }

    @Test
    void shouldPassWhenRequiredFieldPresent() {
        // Arrange
        testData.put("email", "test@example.com");

        // Act
        List<String> errors = formValidationService.validate(testStructure, testData);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== STRING VALIDATION TESTS ====================

    @Test
    void shouldValidateMinLength() {
        // Arrange
        FormField nameField = new FormField();
        nameField.setName("name");
        nameField.setLabel("Full Name");
        nameField.setType("text");
        nameField.setRequired(true);
        nameField.setMinLength(3);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(nameField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("name", "ab"); // Too short

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(error -> error.contains("Full Name") && error.contains("at least"));
    }

    @Test
    void shouldValidateMaxLength() {
        // Arrange
        FormField nameField = new FormField();
        nameField.setName("name");
        nameField.setLabel("Full Name");
        nameField.setType("text");
        nameField.setRequired(true);
        nameField.setMaxLength(10);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(nameField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("name", "this is way too long"); // Too long

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(error -> error.contains("Full Name") && error.contains("must not exceed"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "test sectionId", "exactly 10c"})
    void shouldPassWhenStringLengthValid(String value) {
        // Arrange
        FormField nameField = new FormField();
        nameField.setName("name");
        nameField.setType("text");
        nameField.setRequired(true);
        nameField.setMinLength(3);
        nameField.setMaxLength(15);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(nameField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("name", value);

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldValidatePatternMatch() {
        // Arrange
        testData.put("email", "invalid-email"); // Doesn't match email pattern

        // Act
        List<String> errors = formValidationService.validate(testStructure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
    }

    @Test
    void shouldTrimWhitespaceBeforeValidation() {
        // Arrange
        testData.put("email", "  test@example.com  ");

        // Act
        List<String> errors = formValidationService.validate(testStructure, testData);

        // Assert - Should pass after trimming
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAllowEmptyOptionalString() {
        // Arrange
        FormField optionalField = new FormField();
        optionalField.setName("optional");
        optionalField.setType("text");
        optionalField.setRequired(false);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(optionalField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("optional", "");

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== NUMBER VALIDATION TESTS ====================

    @Test
    void shouldValidateMinValue() {
        // Arrange
        FormField ageField = new FormField();
        ageField.setName("age");
        ageField.setLabel("Age");
        ageField.setType("number");
        ageField.setRequired(true);
        ageField.setMin("0");

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(ageField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("age", -5); // Below minimum

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(error -> error.contains("Age") && error.contains("at least"));
    }

    @Test
    void shouldValidateMaxValue() {
        // Arrange
        FormField ageField = new FormField();
        ageField.setName("age");
        ageField.setLabel("Age");
        ageField.setType("number");
        ageField.setRequired(true);
        ageField.setMax("120");

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(ageField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("age", 150); // Above maximum

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(error -> error.contains("Age") && error.contains("not exceed"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 25, 50, 100, 120})
    void shouldPassWhenNumberInRange(int value) {
        // Arrange
        FormField ageField = new FormField();
        ageField.setName("age");
        ageField.setLabel("Age");
        ageField.setType("number");
        ageField.setRequired(true);
        ageField.setMin("0");
        ageField.setMax("120");

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(ageField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("age", value);

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldValidateNumberType() {
        // Arrange
        FormField ageField = new FormField();
        ageField.setName("age");
        ageField.setLabel("Age");
        ageField.setType("number");
        ageField.setRequired(true);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(ageField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("age", "not a number"); // Invalid type

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(error -> error.contains("Age") && error.contains("valid number"));
    }

    @Test
    void shouldAllowNullOptionalNumber() {
        // Arrange
        FormField optionalNumber = new FormField();
        optionalNumber.setName("optionalNumber");
        optionalNumber.setType("number");
        optionalNumber.setRequired(false);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(optionalNumber));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        // Not adding optionalNumber to testData

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== EMAIL VALIDATION TESTS ====================

    @ParameterizedTest
    @CsvSource({
        "test@example.com, true",
        "user.sectionId@domain.co.uk, true",
        "invalid-email, false",
        "missing-at-sign.com, false",
        "@no-local-part.com, false",
        "no-domain@, false"
    })
    void shouldValidateEmailFormat(String email, boolean shouldPass) {
        // Arrange
        testData.put("email", email);

        // Act
        List<String> errors = formValidationService.validate(testStructure, testData);

        // Assert
        if (shouldPass) {
            assertThat(errors).isEmpty();
        } else {
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(error -> error.contains("Email Address"));
        }
    }

    @Test
    void shouldFailWhenInvalidEmailFormat() {
        // Arrange
        testData.put("email", "not-an-email");

        // Act
        List<String> errors = formValidationService.validate(testStructure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).contains("Email Address");
    }

    @Test
    void shouldAllowEmptyOptionalEmail() {
        // Arrange
        FormField optionalEmail = new FormField();
        optionalEmail.setName("optionalEmail");
        optionalEmail.setType("email");
        optionalEmail.setRequired(false);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(optionalEmail));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        // Not adding optionalEmail to testData

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== COMPLEX VALIDATION TESTS ====================

    @Test
    void shouldValidateMultipleErrors() {
        // Arrange
        FormField email = new FormField();
        email.setName("email");
        email.setLabel("Email Address");
        email.setType("email");
        email.setRequired(true);

        FormField name = new FormField();
        name.setName("name");
        name.setLabel("Full Name");
        name.setType("text");
        name.setRequired(true);
        name.setMinLength(3);

        FormField age = new FormField();
        age.setName("age");
        age.setLabel("Age");
        age.setType("number");
        age.setRequired(true);
        age.setMin("0");

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(email, name, age));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("email", "invalid");
        testData.put("name", "ab");
        testData.put("age", -1);

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).size().isGreaterThanOrEqualTo(3);
        assertThat(errors).anyMatch(error -> error.contains("Email Address"));
        assertThat(errors).anyMatch(error -> error.contains("Full Name"));
        assertThat(errors).anyMatch(error -> error.contains("Age"));
    }

    @Test
    void shouldValidateNestedSections() {
        // Arrange
        FormField field1 = new FormField();
        field1.setName("field1");
        field1.setLabel("First Field");
        field1.setType("text");
        field1.setRequired(true);

        FormField field2 = new FormField();
        field2.setName("field2");
        field2.setLabel("Second Field");
        field2.setType("text");
        field2.setRequired(true);

        FormSection section1 = new FormSection();
        section1.setSectionId("section1");
        section1.setFields(List.of(field1));

        FormSection section2 = new FormSection();
        section2.setSectionId("section2");
        section2.setFields(List.of(field2));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section1, section2));

        testData.put("field1", "value1");
        // Missing field2

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).hasSize(1);
        assertThat(errors).anyMatch(error -> error.contains("Second Field"));
    }

    @Test
    void shouldValidateAllFields() {
        // Arrange
        FormField requiredField = new FormField();
        requiredField.setName("required");
        requiredField.setType("text");
        requiredField.setRequired(true);
        requiredField.setMinLength(3);
        requiredField.setMaxLength(10);

        FormField optionalField = new FormField();
        optionalField.setName("optional");
        optionalField.setType("text");
        optionalField.setRequired(false);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(requiredField, optionalField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("required", "valid");
        testData.put("optional", "any value");

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldHandleEmptyData() {
        // Arrange
        Map<String, Object> emptyData = new HashMap<>();

        // Act
        List<String> errors = formValidationService.validate(testStructure, emptyData);

        // Assert
        assertThat(errors).isNotEmpty();
    }

    @Test
    void shouldValidateCheckboxField() {
        // Arrange
        FormField checkbox = new FormField();
        checkbox.setName("consent");
        checkbox.setType("checkbox");
        checkbox.setRequired(true);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(checkbox));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("consent", true);

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldFailWhenCheckboxNotChecked() {
        // Arrange
        FormField checkbox = new FormField();
        checkbox.setName("consent");
        checkbox.setType("checkbox");
        checkbox.setRequired(true);

        FormSection section = new FormSection();
        section.setSectionId("section1");
        section.setFields(List.of(checkbox));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));

        testData.put("consent", false);

        // Act
        List<String> errors = formValidationService.validate(structure, testData);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(error -> error.contains("consent") && error.contains("required"));
    }

    // ==================== TEST DATA BUILDERS ====================

    private FormStructure buildFormStructure() {
        FormField emailField = new FormField();
        emailField.setName("email");
        emailField.setType("email");
        emailField.setLabel("Email Address");
        emailField.setRequired(true);
        emailField.setPattern("^[^@]+@[^@]+\\.[^@]+$");

        FormSection section = new FormSection();
        section.setSectionId("personal_info");
        section.setTitle("Personal Information");
        section.setFields(List.of(emailField));

        FormStructure structure = new FormStructure();
        structure.setFormId("test-form");
        structure.setVersion("1.0");
        structure.setSections(List.of(section));
        return structure;
    }
}
