package com.example.policlicabine.service;

import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.mapper.FormTemplateMapper;
import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormSection;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.FormTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive service layer tests for FormTemplateService.
 * Tests CRUD operations, business logic, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class FormTemplateServiceTest {

    @Mock
    private FormTemplateRepository formTemplateRepository;

    @Mock
    private FormTemplateMapper formTemplateMapper;

    @Mock
    private UserService userService;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private FormTemplateService formTemplateService;

    private UUID testTemplateId;
    private UUID testUserId;
    private FormTemplate testTemplate;
    private FormTemplateDto testTemplateDto;
    private FormStructure testStructure;

    @BeforeEach
    void setUp() throws Exception {
        testTemplateId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testStructure = buildFormStructure();
        testTemplate = buildFormTemplate();
        testTemplateDto = buildFormTemplateDto();

        // Manually inject EntityManager into service (since @PersistenceContext is not handled by @InjectMocks)
        var field = FormTemplateService.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(formTemplateService, entityManager);

        // Common EntityManager stubs (lenient - not all tests use this)
        lenient().when(entityManager.getReference(eq(User.class), any(UUID.class))).thenReturn(new User());
    }

    // ==================== CREATE TEMPLATE TESTS ====================

    @Test
    void shouldCreateTemplateSuccessfully() {
        // Arrange
        when(formTemplateRepository.findByCode("GDPR_CONSENT_V1")).thenReturn(Optional.empty());
        when(formTemplateRepository.save(any(FormTemplate.class))).thenReturn(testTemplate);
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        FormTemplateDto result = formTemplateService.createTemplate(
            "GDPR_CONSENT_V1",
            "GDPR Consent Form",
            testStructure,
            FormPurpose.GDPR_CONSENT,
            12,
            testUserId
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("GDPR_CONSENT_V1");
        verify(formTemplateRepository).save(any(FormTemplate.class));
        verify(formTemplateMapper).toDto(testTemplate);
    }

    @Test
    void shouldFailWhenCodeIsNull() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> formTemplateService.createTemplate(
                null,
                "GDPR Consent Form",
                testStructure,
                FormPurpose.GDPR_CONSENT,
                12,
                testUserId
            ));
        assertThat(exception.getMessage()).contains("code is required");
        verify(formTemplateRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenCodeIsDuplicate() {
        // Arrange
        when(formTemplateRepository.findByCode("GDPR_CONSENT_V1"))
            .thenReturn(Optional.of(testTemplate));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> formTemplateService.createTemplate(
                "GDPR_CONSENT_V1",
                "GDPR Consent Form",
                testStructure,
                FormPurpose.GDPR_CONSENT,
                12,
                testUserId
            ));
        assertThat(exception.getMessage()).contains("already exists");
        verify(formTemplateRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenStructureIsNull() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> formTemplateService.createTemplate(
                "GDPR_CONSENT_V1",
                "GDPR Consent Form",
                null,
                FormPurpose.GDPR_CONSENT,
                12,
                testUserId
            ));
        assertThat(exception.getMessage()).contains("structure is required");
        verify(formTemplateRepository, never()).save(any());
    }

    // ==================== PUBLISH TEMPLATE TESTS ====================

    @Test
    void shouldPublishTemplateSuccessfully() {
        // Arrange
        testTemplate.setActive(false);
        when(formTemplateRepository.findById(testTemplateId)).thenReturn(Optional.of(testTemplate));
        when(formTemplateRepository.save(testTemplate)).thenReturn(testTemplate);
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        FormTemplateDto result = formTemplateService.publishTemplate(testTemplateId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(testTemplate.getActive()).isTrue();
        verify(formTemplateRepository).save(testTemplate);
    }

    @Test
    void shouldFailWhenTemplateNotFoundForPublish() {
        // Arrange
        when(formTemplateRepository.findById(testTemplateId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> formTemplateService.publishTemplate(testTemplateId));
        assertThat(exception.getMessage()).contains("not found");
        verify(formTemplateRepository, never()).save(any());
    }

    // ==================== GET LATEST TEMPLATE TESTS ====================

    @Test
    void shouldGetLatestTemplateByPurpose() {
        // Arrange
        when(formTemplateRepository.findLatestByPurpose(FormPurpose.GDPR_CONSENT.name()))
            .thenReturn(Optional.of(testTemplate));
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        FormTemplateDto result =
            formTemplateService.getLatestTemplateByPurpose(FormPurpose.GDPR_CONSENT);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPurpose()).isEqualTo(FormPurpose.GDPR_CONSENT);
        verify(formTemplateRepository).findLatestByPurpose(FormPurpose.GDPR_CONSENT.name());
    }

    @Test
    void shouldReturnEmptyWhenNoPurposeMatch() {
        // Arrange
        when(formTemplateRepository.findLatestByPurpose(FormPurpose.SURGERY_CONSENT.name()))
            .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> formTemplateService.getLatestTemplateByPurpose(FormPurpose.SURGERY_CONSENT));
    }

    @Test
    void shouldOnlyReturnActiveTemplates() {
        // Arrange
        testTemplate.setActive(true);
        testTemplate.setIsDeleted(false);
        testTemplateDto.setActive(true);
        when(formTemplateRepository.findLatestByPurpose(FormPurpose.GDPR_CONSENT.name()))
            .thenReturn(Optional.of(testTemplate));
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        FormTemplateDto result =
            formTemplateService.getLatestTemplateByPurpose(FormPurpose.GDPR_CONSENT);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getActive()).isTrue();
    }

    // ==================== CRUD TESTS ====================

    @Test
    void shouldFindTemplateById() {
        // Arrange
        when(formTemplateRepository.findById(testTemplateId)).thenReturn(Optional.of(testTemplate));
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        FormTemplateDto result = formTemplateService.findById(testTemplateId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testTemplateId);
        verify(formTemplateRepository).findById(testTemplateId);
    }

    @Test
    void shouldFindAllTemplates() {
        // Arrange
        List<FormTemplate> templates = List.of(testTemplate, buildFormTemplate());
        List<FormTemplateDto> dtos = List.of(testTemplateDto, buildFormTemplateDto());
        when(formTemplateRepository.findAll()).thenReturn(templates);
        when(formTemplateMapper.toDto(any(FormTemplate.class)))
            .thenReturn(testTemplateDto)
            .thenReturn(buildFormTemplateDto());

        // Act
        List<FormTemplateDto> result = formTemplateService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        verify(formTemplateRepository).findAll();
    }

    @Test
    void shouldUpdateTemplate() {
        // Arrange
        FormTemplateDto updatedDto = buildFormTemplateDto();
        updatedDto.setName("Updated GDPR Form");
        when(formTemplateRepository.findById(testTemplateId)).thenReturn(Optional.of(testTemplate));
        when(formTemplateRepository.save(testTemplate)).thenReturn(testTemplate);
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(updatedDto);

        // Act
        FormTemplateDto result = formTemplateService.update(testTemplateId, updatedDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated GDPR Form");
        verify(formTemplateRepository).save(testTemplate);
    }

    @Test
    void shouldSoftDeleteTemplate() {
        // Arrange
        when(formTemplateRepository.existsById(testTemplateId)).thenReturn(true);

        // Act
        formTemplateService.deleteById(testTemplateId);

        // Assert
        verify(formTemplateRepository).existsById(testTemplateId);
        verify(formTemplateRepository).deleteById(testTemplateId);
    }

    @Test
    void shouldNotFindDeletedTemplates() {
        // Arrange - service checks isDeleted flag and throws if deleted
        testTemplate.setIsDeleted(true);
        when(formTemplateRepository.findById(testTemplateId)).thenReturn(Optional.of(testTemplate));

        // Act & Assert - service filters deleted templates
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> formTemplateService.findById(testTemplateId));
        assertThat(exception.getMessage()).contains("not found");
    }

    @Test
    void shouldHandleNonExistentId() {
        // Arrange
        when(formTemplateRepository.findById(testTemplateId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> formTemplateService.findById(testTemplateId));
        assertThat(exception.getMessage()).contains("not found");
    }

    // ==================== GET BY PURPOSE TESTS ====================

    @Test
    void shouldGetTemplatesByPurpose() {
        // Arrange
        List<FormTemplate> templates = List.of(testTemplate);
        when(formTemplateRepository.findByPurposeAndActiveTrueAndIsDeletedFalse(FormPurpose.GDPR_CONSENT))
            .thenReturn(templates);
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        List<FormTemplateDto> result =
            formTemplateService.getTemplatesByPurpose(FormPurpose.GDPR_CONSENT);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPurpose()).isEqualTo(FormPurpose.GDPR_CONSENT);
    }

    @Test
    void shouldReturnEmptyListWhenNoPurposeMatch() {
        // Arrange
        when(formTemplateRepository.findByPurposeAndActiveTrueAndIsDeletedFalse(FormPurpose.SURGERY_CONSENT))
            .thenReturn(List.of());

        // Act
        List<FormTemplateDto> result =
            formTemplateService.getTemplatesByPurpose(FormPurpose.SURGERY_CONSENT);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldOnlyReturnActiveAndNonDeletedTemplates() {
        // Arrange
        testTemplate.setActive(true);
        testTemplate.setIsDeleted(false);
        testTemplateDto.setActive(true);
        List<FormTemplate> templates = List.of(testTemplate);
        when(formTemplateRepository.findByPurposeAndActiveTrueAndIsDeletedFalse(FormPurpose.GDPR_CONSENT))
            .thenReturn(templates);
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        List<FormTemplateDto> result =
            formTemplateService.getTemplatesByPurpose(FormPurpose.GDPR_CONSENT);

        // Assert
        assertThat(result).allMatch(dto -> dto.getActive());
    }

    // ==================== EDGE CASES ====================

    @Test
    void shouldHandleNullValidityMonths() {
        // Arrange
        when(formTemplateRepository.findByCode("GDPR_CONSENT_V1")).thenReturn(Optional.empty());
        when(formTemplateRepository.save(any(FormTemplate.class))).thenReturn(testTemplate);
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        FormTemplateDto result = formTemplateService.createTemplate(
            "GDPR_CONSENT_V1",
            "GDPR Consent Form",
            testStructure,
            FormPurpose.GDPR_CONSENT,
            null,
            testUserId
        );

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void shouldTrimWhitespaceInCode() {
        // Arrange - Service trims code before calling repository
        lenient().when(formTemplateRepository.findByCode("GDPR_CONSENT_V1")).thenReturn(Optional.empty());
        lenient().when(formTemplateRepository.findByCode("  GDPR_CONSENT_V1  ")).thenReturn(Optional.empty());
        when(formTemplateRepository.save(any(FormTemplate.class))).thenReturn(testTemplate);
        when(formTemplateMapper.toDto(testTemplate)).thenReturn(testTemplateDto);

        // Act
        FormTemplateDto result = formTemplateService.createTemplate(
            "  GDPR_CONSENT_V1  ",
            "GDPR Consent Form",
            testStructure,
            FormPurpose.GDPR_CONSENT,
            12,
            testUserId
        );

        // Assert
        assertThat(result).isNotNull();
        verify(formTemplateRepository).save(argThat(template ->
            template.getCode().equals("GDPR_CONSENT_V1")
        ));
    }

    // ==================== TEST DATA BUILDERS ====================

    private FormTemplate buildFormTemplate() {
        User creator = new User();
        creator.setUserId(testUserId);

        FormTemplate template = new FormTemplate();
        template.setId(testTemplateId);
        template.setCode("GDPR_CONSENT_V1");
        template.setName("GDPR Consent Form");
        template.setVersion(1);
        template.setActive(false);
        template.setStructure(testStructure);
        template.setPurpose(FormPurpose.GDPR_CONSENT);
        template.setValidityMonths(12);
        template.setCreatedAt(LocalDateTime.now());
        template.setCreatedBy(creator);
        template.setIsDeleted(false);
        return template;
    }

    private FormTemplateDto buildFormTemplateDto() {
        return FormTemplateDto.builder()
            .id(testTemplateId)
            .code("GDPR_CONSENT_V1")
            .name("GDPR Consent Form")
            .version(1)
            .active(false)
            .structure(testStructure)
            .purpose(FormPurpose.GDPR_CONSENT)
            .validityMonths(12)
            .createdAt(LocalDateTime.now())
            .createdByUserId(testUserId)
            .build();
    }

    private FormStructure buildFormStructure() {
        FormField emailField = new FormField();
        emailField.setName("email");
        emailField.setType("email");
        emailField.setLabel("Email Address");
        emailField.setRequired(true);
        emailField.setPattern("^[^@]+@[^@]+\\.[^@]+$");

        FormField consentField = new FormField();
        consentField.setName("consent");
        consentField.setType("checkbox");
        consentField.setLabel("I consent to data processing");
        consentField.setRequired(true);

        FormSection section = new FormSection();
        section.setSectionId("personal_info");
        section.setTitle("Personal Information");
        section.setFields(List.of(emailField, consentField));

        FormStructure structure = new FormStructure();
        structure.setSections(List.of(section));
        return structure;
    }

    private FormStructure buildLargeFormStructure() {
        FormStructure structure = new FormStructure();

        // Create 10 sections with 10 fields each
        List<FormSection> sections = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            FormSection section = new FormSection();
            section.setSectionId("section_" + i);
            section.setTitle("Section " + i);

            List<FormField> fields = new java.util.ArrayList<>();
            for (int j = 0; j < 10; j++) {
                FormField field = new FormField();
                field.setName("field_" + i + "_" + j);
                field.setType("text");
                field.setLabel("Field " + i + " " + j);
                field.setRequired(false);
                fields.add(field);
            }
            section.setFields(fields);
            sections.add(section);
        }
        structure.setSections(sections);
        return structure;
    }
}
