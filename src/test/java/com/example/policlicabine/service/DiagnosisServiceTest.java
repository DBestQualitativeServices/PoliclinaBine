package com.example.policlicabine.service;

import com.example.policlicabine.base.BaseServiceTest;
import com.example.policlicabine.builder.DiagnosisTestBuilder;
import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.DiagnosisDto;
import com.example.policlicabine.entity.Diagnosis;
import com.example.policlicabine.mapper.DiagnosisMapper;
import com.example.policlicabine.repository.DiagnosisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.policlicabine.util.ResultAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DiagnosisService demonstrating:
 * - Simple BaseService usage
 * - Entity retrieval for service-to-service calls
 * - Batch entity operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosisService Unit Tests")
class DiagnosisServiceTest extends BaseServiceTest {

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private DiagnosisMapper diagnosisMapper;

    @InjectMocks
    private DiagnosisService diagnosisService;

    private Diagnosis hypertension;
    private Diagnosis diabetes;
    private DiagnosisDto hypertensionDto;

    @BeforeEach
    void setUp() {
        eventPublisher = createEventPublisher();
        diagnosisService = new DiagnosisService(diagnosisRepository, diagnosisMapper, eventPublisher);

        hypertension = DiagnosisTestBuilder.hypertension().build();
        diabetes = DiagnosisTestBuilder.diabetes().build();

        hypertensionDto = DiagnosisDto.builder()
                .diagnosisId(hypertension.getDiagnosisId())
                .icd10Code(hypertension.getIcd10Code())
                .icd10Description(hypertension.getIcd10Description())
                .build();
    }

    // ========================================================================
    // findById() Tests
    // ========================================================================

    @Test
    @DisplayName("Should find diagnosis by ID successfully")
    void findById_Success() {
        // Given
        UUID diagnosisId = hypertension.getDiagnosisId();
        when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.of(hypertension));
        when(diagnosisMapper.toDto(hypertension)).thenReturn(hypertensionDto);

        // When
        Result<DiagnosisDto> result = diagnosisService.findById(diagnosisId);

        // Then
        assertThat(result)
                .isSuccess()
                .hasValue()
                .hasValueSatisfying(dto -> {
                    assertThat(dto.getIcd10Code()).isEqualTo("I10");
                    assertThat(dto.getIcd10Description()).contains("hypertension");
                });
    }

    @Test
    @DisplayName("Should fail when diagnosis not found")
    void findById_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(diagnosisRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Result<DiagnosisDto> result = diagnosisService.findById(nonExistentId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Diagnosis not found");
    }

    // ========================================================================
    // getEntitiesByIds() Tests - Batch Operations
    // ========================================================================

    @Test
    @DisplayName("Should retrieve multiple diagnoses by IDs for service calls")
    void getEntitiesByIds_Success() {
        // Given
        List<UUID> ids = Arrays.asList(hypertension.getDiagnosisId(), diabetes.getDiagnosisId());
        when(diagnosisRepository.findAllById(ids)).thenReturn(Arrays.asList(hypertension, diabetes));

        // When
        List<Diagnosis> result = diagnosisService.getEntitiesByIds(ids);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(hypertension, diabetes);
        verify(diagnosisRepository).findAllById(ids);
    }

    @Test
    @DisplayName("Should return empty list when no diagnoses found by IDs")
    void getEntitiesByIds_NoMatches_EmptyList() {
        // Given
        List<UUID> ids = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        when(diagnosisRepository.findAllById(ids)).thenReturn(List.of());

        // When
        List<Diagnosis> result = diagnosisService.getEntitiesByIds(ids);

        // Then
        assertThat(result).isEmpty();
        verify(diagnosisRepository).findAllById(ids);
    }

    // ========================================================================
    // validateExists() Tests
    // ========================================================================

    @Test
    @DisplayName("Should validate diagnosis exists")
    void validateExists_Success() {
        // Given
        UUID diagnosisId = hypertension.getDiagnosisId();
        when(diagnosisRepository.existsById(diagnosisId)).thenReturn(true);

        // When
        Result<Void> result = diagnosisService.validateExists(diagnosisId);

        // Then
        assertThat(result).isSuccess();
        verify(diagnosisRepository).existsById(diagnosisId);
    }

    @Test
    @DisplayName("Should fail validation when diagnosis does not exist")
    void validateExists_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(diagnosisRepository.existsById(nonExistentId)).thenReturn(false);

        // When
        Result<Void> result = diagnosisService.validateExists(nonExistentId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Diagnosis not found");
    }

    // ========================================================================
    // update() Tests
    // ========================================================================

    @Test
    @DisplayName("Should update diagnosis description successfully")
    void update_Success() {
        // Given
        UUID diagnosisId = hypertension.getDiagnosisId();
        DiagnosisDto updateDto = DiagnosisDto.builder()
                .diagnosisId(diagnosisId)
                .icd10Code("I10")
                .icd10Description("Essential (primary) hypertension - updated")
                .build();

        when(diagnosisRepository.findById(diagnosisId)).thenReturn(Optional.of(hypertension));
        when(diagnosisRepository.save(any(Diagnosis.class))).thenReturn(hypertension);
        when(diagnosisMapper.toDto(any(Diagnosis.class))).thenReturn(updateDto);

        // When
        Result<DiagnosisDto> result = diagnosisService.update(diagnosisId, updateDto);

        // Then
        assertThat(result).isSuccess().hasValue();

        // Verify description was updated
        assertThat(hypertension.getIcd10Description()).contains("updated");
        verify(diagnosisRepository).save(hypertension);
    }

    // ========================================================================
    // deleteById() Tests
    // ========================================================================

    @Test
    @DisplayName("Should delete diagnosis successfully")
    void deleteById_Success() {
        // Given
        UUID diagnosisId = hypertension.getDiagnosisId();
        when(diagnosisRepository.existsById(diagnosisId)).thenReturn(true);
        doNothing().when(diagnosisRepository).deleteById(diagnosisId);

        // When
        Result<Void> result = diagnosisService.deleteById(diagnosisId);

        // Then
        assertThat(result).isSuccess();
        verify(diagnosisRepository).deleteById(diagnosisId);
    }

    @Test
    @DisplayName("Should fail delete when diagnosis not found")
    void deleteById_NotFound_Failure() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(diagnosisRepository.existsById(nonExistentId)).thenReturn(false);

        // When
        Result<Void> result = diagnosisService.deleteById(nonExistentId);

        // Then
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Diagnosis not found");

        verify(diagnosisRepository, never()).deleteById(any());
    }
}
