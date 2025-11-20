package com.example.policlicabine.mapper;

import com.example.policlicabine.builder.PatientTestBuilder;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PatientMapper demonstrating:
 * - MapStruct entity to DTO mapping
 * - MapStruct DTO to entity mapping
 * - Null safety
 * - Bidirectional relationship handling (@Mapping ignore)
 * - Field-level mapping verification
 */
@SpringBootTest(classes = {PatientMapperImpl.class})
@ActiveProfiles("test")
@DisplayName("PatientMapper Tests")
class PatientMapperTest {

    @Autowired
    private PatientMapper patientMapper;

    // ========================================================================
    // toDto() Tests - Entity to DTO
    // ========================================================================

    @Test
    @DisplayName("Should map Patient entity to PatientDto")
    void toDto_Success() {
        // Given
        Patient patient = PatientTestBuilder.aPatient()
                .withFirstName("John")
                .withLastName("Doe")
                .withPhone("0700123456")
                .withEmail("john.doe@test.com")
                .withAddress("123 Test Street")
                .build();

        // When
        PatientDto dto = patientMapper.toDto(patient);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getPatientId()).isEqualTo(patient.getPatientId());
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getPhone()).isEqualTo("0700123456");
        assertThat(dto.getEmail()).isEqualTo("john.doe@test.com");
        assertThat(dto.getAddress()).isEqualTo("123 Test Street");
        assertThat(dto.getRegistrationDate()).isEqualTo(patient.getRegistrationDate());
    }

    @Test
    @DisplayName("Should map Patient with null optional fields to DTO")
    void toDto_NullOptionalFields_Success() {
        // Given
        Patient patient = Patient.builder()
                .patientId(java.util.UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .phone("0700123456")
                .email(null) // Null email
                .address(null) // Null address
                .registrationDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        // When
        PatientDto dto = patientMapper.toDto(patient);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getEmail()).isNull();
        assertThat(dto.getAddress()).isNull();
        assertThat(dto.getFirstName()).isEqualTo("John"); // Required fields still mapped
    }

    @Test
    @DisplayName("Should handle null Patient gracefully")
    void toDto_NullPatient_ReturnsNull() {
        // When
        PatientDto dto = patientMapper.toDto(null);

        // Then
        assertThat(dto).isNull();
    }

    // ========================================================================
    // toEntity() Tests - DTO to Entity
    // ========================================================================

    @Test
    @DisplayName("Should map PatientDto to Patient entity")
    void toEntity_Success() {
        // Given
        PatientDto dto = PatientDto.builder()
                .patientId(java.util.UUID.randomUUID())
                .firstName("Jane")
                .lastName("Smith")
                .phone("0700999888")
                .email("jane.smith@test.com")
                .address("456 Another Street")
                .registrationDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        // When
        Patient entity = patientMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getPatientId()).isEqualTo(dto.getPatientId());
        assertThat(entity.getFirstName()).isEqualTo("Jane");
        assertThat(entity.getLastName()).isEqualTo("Smith");
        assertThat(entity.getPhone()).isEqualTo("0700999888");
        assertThat(entity.getEmail()).isEqualTo("jane.smith@test.com");
        assertThat(entity.getAddress()).isEqualTo("456 Another Street");
        assertThat(entity.getRegistrationDate()).isEqualTo(dto.getRegistrationDate());
    }

    @Test
    @DisplayName("Should ignore appointments collection when mapping to entity")
    void toEntity_AppointmentsIgnored() {
        // Given
        PatientDto dto = PatientDto.builder()
                .patientId(java.util.UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .phone("0700123456")
                .email("john.doe@test.com")
                .registrationDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        // When
        Patient entity = patientMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        // Appointments should be null or empty (ignored by MapStruct)
        // This prevents circular reference issues
        assertThat(entity.getAppointments()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should handle null DTO gracefully")
    void toEntity_NullDto_ReturnsNull() {
        // When
        Patient entity = patientMapper.toEntity(null);

        // Then
        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Should handle DTO with null optional fields")
    void toEntity_NullOptionalFields_Success() {
        // Given
        PatientDto dto = PatientDto.builder()
                .patientId(java.util.UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .phone("0700123456")
                .email(null)
                .address(null)
                .registrationDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        // When
        Patient entity = patientMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getEmail()).isNull();
        assertThat(entity.getAddress()).isNull();
    }

    // ========================================================================
    // Round-trip Tests - Entity -> DTO -> Entity
    // ========================================================================

    @Test
    @DisplayName("Should preserve data through round-trip conversion")
    void roundTrip_PreservesData() {
        // Given
        Patient originalPatient = PatientTestBuilder.aPatient()
                .withFirstName("Alice")
                .withLastName("Wonder")
                .withPhone("0700111222")
                .withEmail("alice@test.com")
                .withAddress("Wonderland Street 1")
                .build();

        // When - Convert to DTO and back to Entity
        PatientDto dto = patientMapper.toDto(originalPatient);
        Patient roundTrippedPatient = patientMapper.toEntity(dto);

        // Then - Core fields should match (except appointments which is ignored)
        assertThat(roundTrippedPatient.getPatientId()).isEqualTo(originalPatient.getPatientId());
        assertThat(roundTrippedPatient.getFirstName()).isEqualTo(originalPatient.getFirstName());
        assertThat(roundTrippedPatient.getLastName()).isEqualTo(originalPatient.getLastName());
        assertThat(roundTrippedPatient.getPhone()).isEqualTo(originalPatient.getPhone());
        assertThat(roundTrippedPatient.getEmail()).isEqualTo(originalPatient.getEmail());
        assertThat(roundTrippedPatient.getAddress()).isEqualTo(originalPatient.getAddress());
        assertThat(roundTrippedPatient.getRegistrationDate()).isEqualTo(originalPatient.getRegistrationDate());
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Should handle empty strings in Patient fields")
    void toDto_EmptyStrings_Mapped() {
        // Given
        Patient patient = Patient.builder()
                .patientId(java.util.UUID.randomUUID())
                .firstName("")
                .lastName("")
                .phone("")
                .email("")
                .address("")
                .registrationDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        // When
        PatientDto dto = patientMapper.toDto(patient);

        // Then - Empty strings should be preserved
        assertThat(dto.getFirstName()).isEmpty();
        assertThat(dto.getLastName()).isEmpty();
        assertThat(dto.getPhone()).isEmpty();
        assertThat(dto.getEmail()).isEmpty();
    }

    @Test
    @DisplayName("Should handle special characters in Patient fields")
    void toDto_SpecialCharacters_Preserved() {
        // Given
        Patient patient = PatientTestBuilder.aPatient()
                .withFirstName("José")
                .withLastName("O'Brien-Smith")
                .withEmail("josé.o'brien@test.com")
                .withAddress("Str. Ştefan cel Mare, №123")
                .build();

        // When
        PatientDto dto = patientMapper.toDto(patient);

        // Then - Special characters should be preserved
        assertThat(dto.getFirstName()).isEqualTo("José");
        assertThat(dto.getLastName()).isEqualTo("O'Brien-Smith");
        assertThat(dto.getEmail()).isEqualTo("josé.o'brien@test.com");
        assertThat(dto.getAddress()).isEqualTo("Str. Ştefan cel Mare, №123");
    }
}
