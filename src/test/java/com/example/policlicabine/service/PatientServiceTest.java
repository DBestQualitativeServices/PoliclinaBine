package com.example.policlicabine.service;

import com.example.policlicabine.base.BaseServiceTest;
import com.example.policlicabine.builder.PatientTestBuilder;
import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.event.PatientRegistered;
import com.example.policlicabine.mapper.PatientMapper;
import com.example.policlicabine.repository.PatientRepository;
import com.example.policlicabine.specification.PatientSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.example.policlicabine.util.ResultAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PatientService demonstrating:
 * - Result pattern assertions
 * - Domain event verification
 * - BaseService inherited methods
 * - Mockito best practices
 * - Test data builders
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService Unit Tests")
class PatientServiceTest extends BaseServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private PatientSpecificationBuilder specificationBuilder;

    @InjectMocks
    private PatientService patientService;

    private Patient testPatient;
    private PatientDto testPatientDto;

    @BeforeEach
    void setUp() {
        // Reset event publisher for each test
        eventPublisher = createEventPublisher();

        // Manually inject eventPublisher since @InjectMocks doesn't handle it
        patientService = new PatientService(patientRepository, patientMapper, eventPublisher, specificationBuilder);

        // Create test data
        testPatient = PatientTestBuilder.aPatient()
                .withFirstName("John")
                .withLastName("Doe")
                .withPhone("0700123456")
                .withEmail("john.doe@test.com")
                .build();

        testPatientDto = PatientDto.builder()
                .patientId(testPatient.getPatientId())
                .firstName(testPatient.getFirstName())
                .lastName(testPatient.getLastName())
                .phone(testPatient.getPhone())
                .email(testPatient.getEmail())
                .build();
    }

    // ========================================================================
    // registerNewPatient() Tests
    // ========================================================================

    @Test
    @DisplayName("Should register new patient successfully")
    void registerNewPatient_Success() {
        // Given
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);
        when(patientMapper.toDto(any(Patient.class))).thenReturn(testPatientDto);

        // When
        Result<PatientDto> result = patientService.registerNewPatient(
                "John", "Doe", "0700123456", "john.doe@test.com", "123 Test St"
        );

        // Then - Use custom Result assertions
        assertThat(result)
                .isSuccess()
                .hasValue()
                .hasValueSatisfying(dto -> {
                    assertThat(dto.getFirstName()).isEqualTo("John");
                    assertThat(dto.getLastName()).isEqualTo("Doe");
                    assertThat(dto.getPhone()).isEqualTo("0700123456");
                });

        // Verify repository interactions
        verify(patientRepository).save(any(Patient.class));

        // Verify domain event was published
        ArgumentCaptor<PatientRegistered> eventCaptor = ArgumentCaptor.forClass(PatientRegistered.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PatientRegistered event = eventCaptor.getValue();
        assertThat(event.patientId()).isEqualTo(testPatient.getPatientId());
        assertThat(event.firstName()).isEqualTo("John");
        assertThat(event.lastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should fail when first name is missing")
    void registerNewPatient_MissingFirstName_Failure() {
        // When
        Result<PatientDto> result = patientService.registerNewPatient(
                null, "Doe", "0700123456", "john.doe@test.com", "123 Test St"
        );

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("First name is required");

        // Verify no database interaction occurred
        verifyNoInteractions(patientRepository);
        verifyNoInteractions(eventPublisher);
    }

    // ========================================================================
    // findById() Tests (Inherited from BaseService)
    // ========================================================================

    @Test
    @DisplayName("Should find patient by ID successfully")
    void findById_Success() {
        // Given
        UUID patientId = testPatient.getPatientId();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(testPatient));
        when(patientMapper.toDto(testPatient)).thenReturn(testPatientDto);

        // When
        Result<PatientDto> result = patientService.findById(patientId);

        // Then
        assertThat(result)
                .isSuccess()
                .hasValue(testPatientDto);

        verify(patientRepository).findById(patientId);
        verify(patientMapper).toDto(testPatient);
    }

    @Test
    @DisplayName("Should fail when patient not found by ID")
    void findById_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(patientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Result<PatientDto> result = patientService.findById(nonExistentId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Patient not found");

        verify(patientRepository).findById(nonExistentId);
        verifyNoInteractions(patientMapper);
    }

    // ========================================================================
    // validateExists() Tests (Inherited from BaseService)
    // ========================================================================

    @Test
    @DisplayName("Should validate patient exists successfully")
    void validateExists_Success() {
        // Given
        UUID patientId = testPatient.getPatientId();
        when(patientRepository.existsById(patientId)).thenReturn(true);

        // When
        Result<Void> result = patientService.validateExists(patientId);

        // Then
        assertThat(result).isSuccess();
        verify(patientRepository).existsById(patientId);
    }

    @Test
    @DisplayName("Should fail validation when patient does not exist")
    void validateExists_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(patientRepository.existsById(nonExistentId)).thenReturn(false);

        // When
        Result<Void> result = patientService.validateExists(nonExistentId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Patient not found");

        verify(patientRepository).existsById(nonExistentId);
    }

    // ========================================================================
    // update() Tests (Inherited from BaseService)
    // ========================================================================

    @Test
    @DisplayName("Should update patient successfully")
    void update_Success() {
        // Given
        UUID patientId = testPatient.getPatientId();
        PatientDto updateDto = PatientDto.builder()
                .patientId(patientId)
                .firstName("Jane")
                .lastName("Smith")
                .phone("0700999888")
                .email("jane.smith@test.com")
                .address("456 New Address")
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);
        when(patientMapper.toDto(any(Patient.class))).thenReturn(updateDto);

        // When
        Result<PatientDto> result = patientService.update(patientId, updateDto);

        // Then
        assertThat(result).isSuccess().hasValue();

        // Verify entity was updated
        verify(patientRepository).findById(patientId);
        verify(patientRepository).save(testPatient);

        // Verify fields were updated on the entity
        assertThat(testPatient.getFirstName()).isEqualTo("Jane");
        assertThat(testPatient.getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Should fail update when patient not found")
    void update_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        PatientDto updateDto = PatientDto.builder().build();

        when(patientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Result<PatientDto> result = patientService.update(nonExistentId, updateDto);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Patient not found");

        verify(patientRepository).findById(nonExistentId);
        verify(patientRepository, never()).save(any());
    }

    // ========================================================================
    // deleteById() Tests (Inherited from BaseService)
    // ========================================================================

    @Test
    @DisplayName("Should delete patient successfully")
    void deleteById_Success() {
        // Given
        UUID patientId = testPatient.getPatientId();
        when(patientRepository.existsById(patientId)).thenReturn(true);
        doNothing().when(patientRepository).deleteById(patientId);

        // When
        Result<Void> result = patientService.deleteById(patientId);

        // Then
        assertThat(result).isSuccess();
        verify(patientRepository).existsById(patientId);
        verify(patientRepository).deleteById(patientId);
    }

    @Test
    @DisplayName("Should fail delete when patient not found")
    void deleteById_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(patientRepository.existsById(nonExistentId)).thenReturn(false);

        // When
        Result<Void> result = patientService.deleteById(nonExistentId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Patient not found");

        verify(patientRepository).existsById(nonExistentId);
        verify(patientRepository, never()).deleteById(any());
    }
}
