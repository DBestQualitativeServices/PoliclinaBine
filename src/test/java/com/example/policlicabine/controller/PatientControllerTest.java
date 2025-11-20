package com.example.policlicabine.controller;

import com.example.policlicabine.builder.PatientTestBuilder;
import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.repository.PatientRepository;
import com.example.policlicabine.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static com.example.policlicabine.util.ResultAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Patient Controller operations.
 *
 * <p>Tests complete patient workflow through service layer:
 * <ul>
 *   <li>Patient registration (CREATE)</li>
 *   <li>Patient retrieval (READ)</li>
 *   <li>Patient update (UPDATE)</li>
 *   <li>Patient deletion (DELETE)</li>
 * </ul>
 *
 * <p><strong>Complementary to PatientServiceTest:</strong>
 * <ul>
 *   <li>PatientServiceTest: Unit tests with mocks for service business logic</li>
 *   <li>PatientControllerTest: Integration tests with real database for CRUD operations</li>
 * </ul>
 *
 * <p><strong>Test Approach:</strong>
 * <ul>
 *   <li>@SpringBootTest - Full Spring context</li>
 *   <li>Real service layer (no mocks)</li>
 *   <li>Real H2 in-memory database</li>
 *   <li>Tests both success and failure paths</li>
 * </ul>
 *
 * @see com.example.policlicabine.service.PatientServiceTest
 * @see com.example.policlicabine.controller.PatientRegistrationWorkflowTest
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Patient Controller Integration Tests")
class PatientControllerTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        patientRepository.deleteAll();
    }

    // ========================================================================
    // POST /api/patients - registerPatient() Tests
    // ========================================================================

    @Test
    @DisplayName("POST /api/patients - Valid patient registration creates patient in database")
    void registerPatient_ValidData_CreatesPatientSuccessfully() {
        // When - Register new patient via service (simulating controller call)
        Result<PatientDto> result = patientService.registerNewPatient(
                "John",
                "Doe",
                "0700123456",
                "john.doe@test.com",
                "123 Test Street"
        );

        // Then - Registration successful
        assertThat(result)
                .isSuccess()
                .hasValue()
                .hasValueSatisfying(dto -> {
                    assertThat(dto.getPatientId()).isNotNull();
                    assertThat(dto.getFirstName()).isEqualTo("John");
                    assertThat(dto.getLastName()).isEqualTo("Doe");
                    assertThat(dto.getPhone()).isEqualTo("0700123456");
                    assertThat(dto.getEmail()).isEqualTo("john.doe@test.com");
                    assertThat(dto.getAddress()).isEqualTo("123 Test Street");
                });

        // Verify patient persisted in database
        assertThat(patientRepository.findAll()).hasSize(1);
        Patient savedPatient = patientRepository.findAll().get(0);
        assertThat(savedPatient.getFirstName()).isEqualTo("John");
        assertThat(savedPatient.getPhone()).isEqualTo("0700123456");
    }

    @Test
    @DisplayName("POST /api/patients - Duplicate phone number returns failure")
    void registerPatient_DuplicatePhone_ReturnsFailure() {
        // Given - Patient with phone already exists
        patientService.registerNewPatient(
                "Jane",
                "Smith",
                "0700123456",
                "jane.smith@test.com",
                "456 Another St"
        );

        // When - Attempt to register another patient with same phone
        Result<PatientDto> result = patientService.registerNewPatient(
                "John",
                "Doe",
                "0700123456",  // Duplicate phone
                "john.doe@test.com",
                "789 Different St"
        );

        // Then - Registration fails with error message
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("phone number already exists");

        // Verify only one patient in database
        assertThat(patientRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/patients - Missing first name returns failure")
    void registerPatient_MissingFirstName_ReturnsFailure() {
        // When - Register with null first name
        Result<PatientDto> result = patientService.registerNewPatient(
                null,  // Missing first name
                "Doe",
                "0700123456",
                "john.doe@test.com",
                "123 Test St"
        );

        // Then - Registration fails
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("First name is required");

        // Verify no patient created
        assertThat(patientRepository.findAll()).isEmpty();
    }

    // ========================================================================
    // GET /api/patients/{patientId} - getPatient() Tests
    // ========================================================================

    @Test
    @DisplayName("GET /api/patients/{id} - Valid ID retrieves patient successfully")
    void getPatient_ValidId_ReturnsPatientDto() {
        // Given - Patient exists in database
        Result<PatientDto> createResult = patientService.registerNewPatient(
                "Alice",
                "Johnson",
                "0700111222",
                "alice.johnson@test.com",
                "111 First St"
        );

        UUID patientId = createResult.getValue().getPatientId();

        // When - Retrieve patient by ID
        Result<PatientDto> result = patientService.findById(patientId);

        // Then - Patient retrieved successfully
        assertThat(result)
                .isSuccess()
                .hasValue()
                .hasValueSatisfying(dto -> {
                    assertThat(dto.getPatientId()).isEqualTo(patientId);
                    assertThat(dto.getFirstName()).isEqualTo("Alice");
                    assertThat(dto.getLastName()).isEqualTo("Johnson");
                    assertThat(dto.getEmail()).isEqualTo("alice.johnson@test.com");
                });
    }

    @Test
    @DisplayName("GET /api/patients/{id} - Non-existent ID returns failure")
    void getPatient_NonExistentId_ReturnsFailure() {
        // Given - Random UUID that doesn't exist
        UUID nonExistentId = UUID.randomUUID();

        // When - Attempt to retrieve non-existent patient
        Result<PatientDto> result = patientService.findById(nonExistentId);

        // Then - Retrieval fails
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Patient not found");
    }

    // ========================================================================
    // PUT /api/patients/{patientId} - updatePatient() Tests
    // ========================================================================

    @Test
    @DisplayName("PUT /api/patients/{id} - Valid update modifies patient successfully")
    void updatePatient_ValidData_UpdatesPatientSuccessfully() {
        // Given - Patient exists
        Result<PatientDto> createResult = patientService.registerNewPatient(
                "Bob",
                "Williams",
                "0700333444",
                "bob.old@test.com",
                "222 Second St"
        );

        UUID patientId = createResult.getValue().getPatientId();

        // When - Update patient details
        PatientDto updateDto = PatientDto.builder()
                .patientId(patientId)
                .firstName("Robert")  // Changed name
                .lastName("Williams")
                .phone("0700333444")
                .email("bob.new@test.com")  // Changed email
                .address("333 New Address")  // Changed address
                .build();

        Result<PatientDto> result = patientService.update(patientId, updateDto);

        // Then - Update successful
        assertThat(result)
                .isSuccess()
                .hasValue()
                .hasValueSatisfying(dto -> {
                    assertThat(dto.getFirstName()).isEqualTo("Robert");
                    assertThat(dto.getEmail()).isEqualTo("bob.new@test.com");
                    assertThat(dto.getAddress()).isEqualTo("333 New Address");
                });

        // Verify database reflects changes
        Optional<Patient> updatedPatient = patientRepository.findById(patientId);
        assertThat(updatedPatient).isPresent();
        assertThat(updatedPatient.get().getFirstName()).isEqualTo("Robert");
        assertThat(updatedPatient.get().getEmail()).isEqualTo("bob.new@test.com");
    }

    @Test
    @DisplayName("PUT /api/patients/{id} - Non-existent patient returns failure")
    void updatePatient_NonExistentId_ReturnsFailure() {
        // Given - Random UUID that doesn't exist
        UUID nonExistentId = UUID.randomUUID();

        PatientDto updateDto = PatientDto.builder()
                .patientId(nonExistentId)
                .firstName("John")
                .lastName("Doe")
                .phone("0700123456")
                .email("test@example.com")
                .build();

        // When - Attempt to update non-existent patient
        Result<PatientDto> result = patientService.update(nonExistentId, updateDto);

        // Then - Update fails
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Patient not found");
    }

    // ========================================================================
    // DELETE /api/patients/{patientId} - deletePatient() Tests
    // ========================================================================

    @Test
    @DisplayName("DELETE /api/patients/{id} - Valid delete removes patient from database")
    void deletePatient_ValidId_DeletesPatientSuccessfully() {
        // Given - Patient exists
        Result<PatientDto> createResult = patientService.registerNewPatient(
                "Charlie",
                "Brown",
                "0700555666",
                "charlie.brown@test.com",
                "444 Fourth St"
        );

        UUID patientId = createResult.getValue().getPatientId();

        // Verify patient exists before deletion
        assertThat(patientRepository.findById(patientId)).isPresent();

        // When - Delete patient
        Result<Void> result = patientService.deleteById(patientId);

        // Then - Deletion successful
        assertThat(result).isSuccess();

        // Verify patient removed from database
        assertThat(patientRepository.findById(patientId)).isEmpty();
        assertThat(patientRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/patients/{id} - Non-existent patient returns failure")
    void deletePatient_NonExistentId_ReturnsFailure() {
        // Given - Random UUID that doesn't exist
        UUID nonExistentId = UUID.randomUUID();

        // When - Attempt to delete non-existent patient
        Result<Void> result = patientService.deleteById(nonExistentId);

        // Then - Deletion fails
        assertThat(result)
                .isFailure()
                .hasErrorMessageContaining("Patient not found");
    }
}
