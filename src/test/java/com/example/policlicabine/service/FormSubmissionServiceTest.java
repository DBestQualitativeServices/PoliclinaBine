package com.example.policlicabine.service;

import com.example.policlicabine.dto.FormSubmissionDto;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.FormSubmissionMapper;
import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormSection;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.FormSubmissionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive service layer tests for FormSubmissionService.
 * Tests form submission, signing, validation, expiration, and service integration.
 */
@ExtendWith(MockitoExtension.class)
class FormSubmissionServiceTest {

    @Mock
    private FormSubmissionRepository formSubmissionRepository;

    @Mock
    private FormSubmissionMapper formSubmissionMapper;

    @Mock
    private FormTemplateService formTemplateService;

    @Mock
    private PatientService patientService;

    @Mock
    private FormValidationService formValidationService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private FormSubmissionService formSubmissionService;

    private UUID testSubmissionId;
    private UUID testTemplateId;
    private UUID testPatientId;
    private UUID testUserId;
    private UUID testSessionId;
    private UUID testFileId;
    private FormSubmission testSubmission;
    private FormSubmissionDto testSubmissionDto;
    private FormTemplate testTemplate;
    private Patient testPatient;
    private Map<String, Object> testFormData;

    @BeforeEach
    void setUp() throws Exception {
        testSubmissionId = UUID.randomUUID();
        testTemplateId = UUID.randomUUID();
        testPatientId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testSessionId = UUID.randomUUID();
        testFileId = UUID.randomUUID();

        testTemplate = buildFormTemplate();
        testPatient = buildPatient();
        testFormData = buildFormData();
        testSubmission = buildFormSubmission();
        testSubmissionDto = buildFormSubmissionDto();

        // Manually inject EntityManager into service (since @PersistenceContext is not handled by @InjectMocks)
        var field = FormSubmissionService.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(formSubmissionService, entityManager);

        // Common EntityManager stubs (lenient - not all tests use all of these)
        lenient().when(entityManager.getReference(eq(Patient.class), any(UUID.class))).thenReturn(testPatient);
        lenient().when(entityManager.getReference(eq(FormTemplate.class), any(UUID.class))).thenReturn(testTemplate);
        lenient().when(entityManager.getReference(eq(User.class), any(UUID.class))).thenReturn(new User());
        lenient().when(entityManager.getReference(eq(File.class), any(UUID.class))).thenReturn(new File());
    }

    // ==================== SUBMIT FORM TESTS ====================

    @Test
    void shouldSubmitFormSuccessfully() {
        // Arrange
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(formValidationService.validate(any(), any())).thenReturn(List.of());
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenReturn(testSubmission);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        FormSubmissionDto result = formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            testFormData,
            null,
            null,
            testUserId
        );

        // Assert
        assertThat(result).isNotNull();
        verify(formSubmissionRepository).save(any(FormSubmission.class));
    }

    @Test
    void shouldFailWhenTemplateNotFound() {
        // Arrange
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            testFormData,
            null,
            null,
            testUserId
        ));
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenTemplateInactive() {
        // Arrange
        testTemplate.setActive(false);
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            testFormData,
            null,
            null,
            testUserId
        ));
        assertThat(exception.getMessage()).contains("not active");
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenPatientNotFound() {
        // Arrange
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(patientService.existsById(testPatientId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            testFormData,
            null,
            null,
            testUserId
        ));
        assertThat(exception.getMessage()).contains("Patient not found");
        verify(formSubmissionRepository, never()).save(any());
    }

    // ==================== FORM VALIDATION TESTS ====================

    @Test
    void shouldFailWhenRequiredFieldMissing() {
        // Arrange
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(formValidationService.validate(any(), any()))
            .thenReturn(List.of("Field 'email' is required"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            testFormData,
            null,
            null,
            testUserId
        ));
        assertThat(exception.getMessage()).contains("Field 'email' is required");
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenInvalidEmailFormat() {
        // Arrange
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(formValidationService.validate(any(), any()))
            .thenReturn(List.of("Field 'Email Address' must be a valid email address"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            testFormData,
            null,
            null,
            testUserId
        ));
        assertThat(exception.getMessage()).containsAnyOf("valid email", "pattern");
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenNumberOutOfRange() {
        // Arrange
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(formValidationService.validate(any(), any()))
            .thenReturn(List.of("Field 'Age' must not exceed 120"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            testFormData,
            null,
            null,
            testUserId
        ));
        assertThat(exception.getMessage()).containsAnyOf("at least", "not exceed");
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenStringTooLong() {
        // Arrange
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(formValidationService.validate(any(), any()))
            .thenReturn(List.of("Field 'Name' must not exceed 100 characters"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            testFormData,
            null,
            null,
            testUserId
        ));
        assertThat(exception.getMessage()).contains("must not exceed");
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldAllowOptionalFieldsEmpty() {
        // Arrange
        Map<String, Object> dataWithMissingOptional = new HashMap<>(testFormData);
        dataWithMissingOptional.remove("optionalField");

        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(formValidationService.validate(any(), eq(dataWithMissingOptional))).thenReturn(List.of());
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenReturn(testSubmission);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        FormSubmissionDto result = formSubmissionService.submitForm(
            testTemplateId,
            testPatientId,
            dataWithMissingOptional,
            null,
            null,
            testUserId
        );

        // Assert
        assertThat(result).isNotNull();
        verify(formSubmissionRepository).save(any(FormSubmission.class));
    }

    // ==================== SIGN FORM TESTS ====================

    @Test
    void shouldSignFormSuccessfully() {
        // Arrange
        testSubmission.setPatientSignedAt(null);
        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));
        when(formSubmissionRepository.save(testSubmission)).thenReturn(testSubmission);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        FormSubmissionDto result =
            formSubmissionService.signForm(testSubmissionId, testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(testSubmission.getPatientSignedAt()).isNotNull();
        verify(formSubmissionRepository).save(testSubmission);
    }

    @Test
    void shouldRecordSignatureTimestamp() {
        // Arrange
        testSubmission.setPatientSignedAt(null);
        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));
        when(formSubmissionRepository.save(testSubmission)).thenReturn(testSubmission);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        formSubmissionService.signForm(testSubmissionId, testUserId);

        // Assert
        assertThat(testSubmission.getPatientSignedAt()).isNotNull();
    }

    @Test
    void shouldRecordWitness() {
        // Arrange
        User witness = new User();
        witness.setUserId(testUserId);
        testSubmission.setPatientSignedAt(null);
        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));
        lenient().when(entityManager.getReference(eq(User.class), eq(testUserId))).thenReturn(witness);
        when(formSubmissionRepository.save(testSubmission)).thenReturn(testSubmission);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        formSubmissionService.signForm(testSubmissionId, testUserId);

        // Assert
        verify(formSubmissionRepository).save(testSubmission);
    }

    @Test
    void shouldFailWhenAlreadySigned() {
        // Arrange
        testSubmission.setPatientSignedAt(LocalDateTime.now());
        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () ->
            formSubmissionService.signForm(testSubmissionId, testUserId)
        );
        assertThat(exception.getMessage()).contains("already signed");
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenSubmissionNotFound() {
        // Arrange
        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            formSubmissionService.signForm(testSubmissionId, testUserId)
        );
        assertThat(exception.getMessage()).contains("not found");
        verify(formSubmissionRepository, never()).save(any());
    }

    // ==================== HAS VALID FORM TESTS ====================

    @Test
    void shouldReturnTrueWhenValidFormExists() {
        // Arrange
        testSubmission.setPatientSignedAt(LocalDateTime.now());
        testSubmission.setExpiresAt(LocalDateTime.now().plusMonths(6));
        when(formSubmissionRepository.findValidFormByPatientAndPurpose(
            eq(testPatientId),
            eq(FormPurpose.GDPR_CONSENT.name()),
            any(LocalDateTime.class)))
            .thenReturn(Optional.of(testSubmission));

        // Act
        boolean result =
            formSubmissionService.hasValidForm(testPatientId, FormPurpose.GDPR_CONSENT);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoFormExists() {
        // Arrange
        when(formSubmissionRepository.findValidFormByPatientAndPurpose(
            eq(testPatientId),
            eq(FormPurpose.SURGERY_CONSENT.name()),
            any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // Act
        boolean result =
            formSubmissionService.hasValidForm(testPatientId, FormPurpose.SURGERY_CONSENT);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenFormExpired() {
        // Arrange
        testSubmission.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(formSubmissionRepository.findValidFormByPatientAndPurpose(
            eq(testPatientId),
            eq(FormPurpose.GDPR_CONSENT.name()),
            any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // Act
        boolean result =
            formSubmissionService.hasValidForm(testPatientId, FormPurpose.GDPR_CONSENT);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenFormNotSigned() {
        // Arrange
        testSubmission.setPatientSignedAt(null);
        when(formSubmissionRepository.findValidFormByPatientAndPurpose(
            eq(testPatientId),
            eq(FormPurpose.GDPR_CONSENT.name()),
            any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // Act
        boolean result =
            formSubmissionService.hasValidForm(testPatientId, FormPurpose.GDPR_CONSENT);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenFormDeleted() {
        // Arrange
        testSubmission.setIsDeleted(true);
        when(formSubmissionRepository.findValidFormByPatientAndPurpose(
            eq(testPatientId),
            eq(FormPurpose.GDPR_CONSENT.name()),
            any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // Act
        boolean result =
            formSubmissionService.hasValidForm(testPatientId, FormPurpose.GDPR_CONSENT);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== GET FORMS TESTS ====================

    @Test
    void shouldGetFormsByPatient() {
        // Arrange
        List<FormSubmission> submissions = List.of(testSubmission);
        when(formSubmissionRepository.findByPatientPatientIdAndIsDeletedFalse(testPatientId))
            .thenReturn(submissions);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        List<FormSubmissionDto> result =
            formSubmissionService.getFormsByPatient(testPatientId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientId()).isEqualTo(testPatientId);
    }

    @Test
    void shouldGetFormsBySession() {
        // Arrange
        List<FormSubmission> submissions = List.of(testSubmission);
        when(formSubmissionRepository.findByAppointmentSessionSessionIdAndIsDeletedFalse(testSessionId))
            .thenReturn(submissions);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        List<FormSubmissionDto> result =
            formSubmissionService.getFormsBySession(testSessionId);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldUseEntityGraphForFormRetrieval() {
        // Arrange
        when(formSubmissionRepository.findWithDetailsById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        FormSubmissionDto result = formSubmissionService.findById(testSubmissionId);

        // Assert
        assertThat(result).isNotNull();
        verify(formSubmissionRepository).findWithDetailsById(testSubmissionId);
    }

    @Test
    void shouldExcludeDeletedForms() {
        // Arrange
        testSubmission.setIsDeleted(true);
        when(formSubmissionRepository.findWithDetailsById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            formSubmissionService.findById(testSubmissionId)
        );
    }

    // ==================== FILE ATTACHMENT TESTS ====================

    @Test
    void shouldAttachFileSuccessfully() {
        // Arrange
        File file = new File();
        file.setId(testFileId);
        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));
        when(entityManager.getReference(File.class, testFileId)).thenReturn(file);
        when(formSubmissionRepository.save(testSubmission)).thenReturn(testSubmission);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        FormSubmissionDto result =
            formSubmissionService.attachFile(testSubmissionId, testFileId);

        // Assert
        assertThat(result).isNotNull();
        verify(formSubmissionRepository).save(testSubmission);
    }

    @Test
    void shouldFailWhenFileNotFound() {
        // Arrange
        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));
        when(entityManager.getReference(File.class, testFileId)).thenThrow(new RuntimeException("File not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            formSubmissionService.attachFile(testSubmissionId, testFileId)
        );
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenSubmissionNotFoundForAttachment() {
        // Arrange
        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            formSubmissionService.attachFile(testSubmissionId, testFileId)
        );
        assertThat(exception.getMessage()).contains("not found");
        verify(formSubmissionRepository, never()).save(any());
    }

    @Test
    void shouldHandleMultipleAttachments() {
        // Arrange
        File file1 = new File();
        file1.setId(testFileId);
        File file2 = new File();
        file2.setId(UUID.randomUUID());

        when(formSubmissionRepository.findById(testSubmissionId))
            .thenReturn(Optional.of(testSubmission));
        lenient().when(entityManager.getReference(eq(File.class), any(UUID.class))).thenReturn(file1).thenReturn(file2);
        when(formSubmissionRepository.save(testSubmission)).thenReturn(testSubmission);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        formSubmissionService.attachFile(testSubmissionId, file1.getId());
        formSubmissionService.attachFile(testSubmissionId, file2.getId());

        // Assert
        verify(formSubmissionRepository, times(2)).save(testSubmission);
    }

    // ==================== EXPIRATION TESTS ====================

    @Test
    void shouldGetExpiringSoonForms() {
        // Arrange
        List<FormSubmission> submissions = List.of(testSubmission);
        when(formSubmissionRepository.findExpiringSoon(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(submissions);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        List<FormSubmissionDto> result = formSubmissionService.getExpiringSoon(30);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldCalculateExpirationFromTemplate() {
        // Arrange
        testTemplate.setValidityMonths(12);
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(formValidationService.validate(any(), any())).thenReturn(List.of());
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> {
            FormSubmission submission = invocation.getArgument(0);
            assertThat(submission.getExpiresAt()).isNotNull();
            assertThat(submission.getExpiresAt()).isAfter(LocalDateTime.now().plusMonths(11));
            return testSubmission;
        });
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        formSubmissionService.submitForm(testTemplateId, testPatientId, testFormData, null, null, testUserId);

        // Assert
        verify(formSubmissionRepository).save(any(FormSubmission.class));
    }

    @Test
    void shouldHandleNullValidityMonths() {
        // Arrange
        testTemplate.setValidityMonths(null);
        when(formTemplateService.getEntityById(testTemplateId)).thenReturn(testTemplate);
        when(formValidationService.validate(any(), any())).thenReturn(List.of());
        when(formSubmissionRepository.save(any(FormSubmission.class))).thenAnswer(invocation -> {
            FormSubmission submission = invocation.getArgument(0);
            assertThat(submission.getExpiresAt()).isNull();
            return testSubmission;
        });
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        formSubmissionService.submitForm(testTemplateId, testPatientId, testFormData, null, null, testUserId);

        // Assert
        verify(formSubmissionRepository).save(any(FormSubmission.class));
    }

    @Test
    void shouldOrderByExpirationDate() {
        // Arrange
        List<FormSubmission> submissions = List.of(testSubmission);
        when(formSubmissionRepository.findExpiringSoon(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(submissions);
        when(formSubmissionMapper.toDto(testSubmission)).thenReturn(testSubmissionDto);

        // Act
        List<FormSubmissionDto> result = formSubmissionService.getExpiringSoon(30);

        // Assert
        assertThat(result).isNotNull();
        verify(formSubmissionRepository).findExpiringSoon(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // ==================== TEST DATA BUILDERS ====================

    private FormTemplate buildFormTemplate() {
        FormTemplate template = new FormTemplate();
        template.setId(testTemplateId);
        template.setCode("GDPR_CONSENT_V1");
        template.setName("GDPR Consent Form");
        template.setVersion(1);
        template.setActive(true);
        template.setStructure(buildFormStructure());
        template.setPurpose(FormPurpose.GDPR_CONSENT);
        template.setValidityMonths(12);
        template.setIsDeleted(false);
        return template;
    }

    private Patient buildPatient() {
        Patient patient = new Patient();
        patient.setPatientId(testPatientId);
        patient.setFirstName("John");
        patient.setLastName("Doe");
        return patient;
    }

    private FormSubmission buildFormSubmission() {
        FormSubmission submission = new FormSubmission();
        submission.setId(testSubmissionId);
        submission.setTemplate(testTemplate);
        submission.setPatient(testPatient);
        submission.setTemplateSnapshot(testTemplate.getStructure());
        submission.setData(testFormData);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setExpiresAt(LocalDateTime.now().plusMonths(12));
        submission.setIsDeleted(false);
        return submission;
    }

    private FormSubmissionDto buildFormSubmissionDto() {
        return FormSubmissionDto.builder()
            .id(testSubmissionId)
            .templateId(testTemplateId)
            .templateName("GDPR Consent Form")
            .patientId(testPatientId)
            .patientName("John Doe")
            .templateSnapshot(testTemplate.getStructure())
            .data(testFormData)
            .submittedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMonths(12))
            .submittedByUserId(testUserId)
            .build();
    }

    private FormStructure buildFormStructure() {
        FormField emailField = new FormField();
        emailField.setName("email");
        emailField.setType("email");
        emailField.setLabel("Email Address");
        emailField.setRequired(true);

        FormSection section = new FormSection();
        section.setSectionId("personal_info");
        section.setTitle("Personal Information");
        section.setFields(List.of(emailField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));
        return structure;
    }

    private Map<String, Object> buildFormData() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", "john.doe@example.com");
        data.put("consent", true);
        return data;
    }
}
