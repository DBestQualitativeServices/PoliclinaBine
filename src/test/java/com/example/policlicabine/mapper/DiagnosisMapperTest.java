package com.example.policlicabine.mapper;

import com.example.policlicabine.builder.DiagnosisTestBuilder;
import com.example.policlicabine.dto.DiagnosisDto;
import com.example.policlicabine.entity.Diagnosis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DiagnosisMapper demonstrating:
 * - Simple MapStruct mappings
 * - ICD-10 code mapping
 * - Bidirectional relationship handling (sessions ignored)
 */
@SpringBootTest(classes = {DiagnosisMapperImpl.class})
@ActiveProfiles("test")
@DisplayName("DiagnosisMapper Tests")
class DiagnosisMapperTest {

    @Autowired
    private DiagnosisMapper diagnosisMapper;

    // ========================================================================
    // toDto() Tests
    // ========================================================================

    @Test
    @DisplayName("Should map Diagnosis entity to DiagnosisDto")
    void toDto_Success() {
        // Given
        Diagnosis diagnosis = DiagnosisTestBuilder.hypertension().build();

        // When
        DiagnosisDto dto = diagnosisMapper.toDto(diagnosis);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getDiagnosisId()).isEqualTo(diagnosis.getDiagnosisId());
        assertThat(dto.getIcd10Code()).isEqualTo("I10");
        assertThat(dto.getIcd10Description()).isEqualTo("Essential (primary) hypertension");
    }

    @Test
    @DisplayName("Should map diabetes diagnosis to DTO")
    void toDto_Diabetes_Success() {
        // Given
        Diagnosis diagnosis = DiagnosisTestBuilder.diabetes().build();

        // When
        DiagnosisDto dto = diagnosisMapper.toDto(diagnosis);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getIcd10Code()).isEqualTo("E11");
        assertThat(dto.getIcd10Description()).contains("diabetes");
    }

    @Test
    @DisplayName("Should handle null Diagnosis")
    void toDto_Null_ReturnsNull() {
        // When
        DiagnosisDto dto = diagnosisMapper.toDto(null);

        // Then
        assertThat(dto).isNull();
    }

    // ========================================================================
    // toEntity() Tests
    // ========================================================================

    @Test
    @DisplayName("Should map DiagnosisDto to Diagnosis entity")
    void toEntity_Success() {
        // Given
        DiagnosisDto dto = DiagnosisDto.builder()
                .diagnosisId(UUID.randomUUID())
                .icd10Code("J00")
                .icd10Description("Acute nasopharyngitis (common cold)")
                .build();

        // When
        Diagnosis entity = diagnosisMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getDiagnosisId()).isEqualTo(dto.getDiagnosisId());
        assertThat(entity.getIcd10Code()).isEqualTo("J00");
        assertThat(entity.getIcd10Description()).isEqualTo("Acute nasopharyngitis (common cold)");
    }

    @Test
    @DisplayName("Should ignore sessions collection when mapping to entity")
    void toEntity_SessionsIgnored() {
        // Given
        DiagnosisDto dto = DiagnosisDto.builder()
                .diagnosisId(UUID.randomUUID())
                .icd10Code("I10")
                .icd10Description("Hypertension")
                .build();

        // When
        Diagnosis entity = diagnosisMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        // Sessions should be null or empty (ignored to prevent circular references)
        assertThat(entity.getSessions()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should handle null DTO")
    void toEntity_Null_ReturnsNull() {
        // When
        Diagnosis entity = diagnosisMapper.toEntity(null);

        // Then
        assertThat(entity).isNull();
    }

    // ========================================================================
    // Round-trip Tests
    // ========================================================================

    @Test
    @DisplayName("Should preserve data through round-trip conversion")
    void roundTrip_PreservesData() {
        // Given
        Diagnosis original = DiagnosisTestBuilder.acuteUpperRespiratoryInfection().build();

        // When
        DiagnosisDto dto = diagnosisMapper.toDto(original);
        Diagnosis roundTripped = diagnosisMapper.toEntity(dto);

        // Then
        assertThat(roundTripped.getDiagnosisId()).isEqualTo(original.getDiagnosisId());
        assertThat(roundTripped.getIcd10Code()).isEqualTo(original.getIcd10Code());
        assertThat(roundTripped.getIcd10Description()).isEqualTo(original.getIcd10Description());
    }

    // ========================================================================
    // ICD-10 Code Specific Tests
    // ========================================================================

    @Test
    @DisplayName("Should correctly map various ICD-10 code formats")
    void toDto_VariousIcd10Codes_Success() {
        // Test different ICD-10 code formats
        Diagnosis[] diagnoses = {
                DiagnosisTestBuilder.aDiagnosis().withIcd10Code("A00").build(), // Single letter
                DiagnosisTestBuilder.aDiagnosis().withIcd10Code("Z00.0").build(), // With decimal
                DiagnosisTestBuilder.aDiagnosis().withIcd10Code("E11.9").build(), // Complex code
        };

        for (Diagnosis diagnosis : diagnoses) {
            DiagnosisDto dto = diagnosisMapper.toDto(diagnosis);
            assertThat(dto.getIcd10Code()).isEqualTo(diagnosis.getIcd10Code());
        }
    }

    @Test
    @DisplayName("Should handle special characters in description")
    void toDto_SpecialCharactersInDescription_Preserved() {
        // Given
        Diagnosis diagnosis = DiagnosisTestBuilder.aDiagnosis()
                .withIcd10Code("I10")
                .withDescription("Essential (primary) hypertension - Stage 1 & 2")
                .build();

        // When
        DiagnosisDto dto = diagnosisMapper.toDto(diagnosis);

        // Then
        assertThat(dto.getIcd10Description()).isEqualTo("Essential (primary) hypertension - Stage 1 & 2");
    }
}
