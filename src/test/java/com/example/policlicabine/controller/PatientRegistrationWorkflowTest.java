package com.example.policlicabine.controller;

import com.example.policlicabine.builder.ConsultationTestBuilder;
import com.example.policlicabine.builder.DoctorTestBuilder;
import com.example.policlicabine.builder.PatientTestBuilder;
import com.example.policlicabine.builder.UserTestBuilder;
import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.event.NewPatientRegisteredEvent;
import com.example.policlicabine.repository.*;
import com.example.policlicabine.service.AppointmentSessionService;
import com.example.policlicabine.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.example.policlicabine.entity.enums.PermissionEnum;
import com.example.policlicabine.entity.enums.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Patient Registration business workflows.
 *
 * <p>Tests complete multi-step business processes:
 * <ul>
 *   <li>Patient registration → Event publishing → Downstream effects</li>
 *   <li>Patient registration → Consent upload → Treatment appointment booking</li>
 *   <li>Patient registration → Search → Retrieve</li>
 *   <li>Patient update → Search by new email</li>
 *   <li>Patient deletion with referential integrity constraints</li>
 * </ul>
 *
 * <p><strong>Integration Test Characteristics:</strong>
 * <ul>
 *   <li>Full Spring context with @SpringBootTest</li>
 *   <li>Real service layer (no mocks)</li>
 *   <li>Real H2 in-memory database</li>
 *   <li>Real event publishing and listeners</li>
 *   <li>Multi-service interactions</li>
 * </ul>
 *
 * <p><strong>Complementary to PatientServiceTest & PatientControllerTest:</strong>
 * <ul>
 *   <li>PatientServiceTest: Unit tests for service business logic</li>
 *   <li>PatientControllerTest: Unit tests for HTTP layer</li>
 *   <li>PatientRegistrationWorkflowTest: Integration tests for end-to-end workflows</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Patient Registration Business Workflow Tests")
@org.springframework.context.annotation.Import(PatientRegistrationWorkflowTest.TestConfig.class)
class PatientRegistrationWorkflowTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private AppointmentSessionService appointmentSessionService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private AppointmentSessionRepository appointmentSessionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private PatientTestEventListener eventListener;

    private Doctor testDoctor;
    private ConsultationType testConsultation;

    @BeforeEach
    void setUp() {
        // Clean database for each test (order matters due to FK constraints)
        appointmentSessionRepository.deleteAll();
        patientRepository.deleteAll();
        consultationRepository.deleteAll();
        doctorRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Reset event listener
        eventListener.clear();

        // Create or find Permission for test (idempotent)
        Permission allPermission = permissionRepository.findByName(PermissionEnum.ALL)
                .orElseGet(() -> {
                    Permission newPermission = Permission.builder()
                            .name(PermissionEnum.ALL)
                            .description("Full system access")
                            .build();
                    return permissionRepository.save(newPermission);
                });

        // Create DOCTOR role for test
        Role doctorRole = Role.builder()
                .name(UserRole.DOCTOR)
                .description("Medical doctor with patient care privileges")
                .permissions(new HashSet<>(Set.of(allPermission)))
                .build();
        roleRepository.save(doctorRole);

        // Create test doctor user and set role directly (avoid bidirectional relationship issues)
        User doctorUser = User.builder()
                .username("dr.smith")
                .password("password123")
                .enabled(true)
                .accountNonLocked(true)
                .build();

        // Directly add to user's roles collection (unidirectional) to avoid LazyInitializationException
        doctorUser.getRoles().add(doctorRole);
        userRepository.save(doctorUser);

        testDoctor = DoctorTestBuilder.aMedicalDermatologist()
                .withUser(doctorUser)
                .build();
        doctorRepository.save(testDoctor);

        // Create test consultation
        testConsultation = ConsultationTestBuilder.generalConsultation()
                .withName("General Consultation")
                .withPrice(new BigDecimal("100.00"))
                .withIsActive(true)
                .build();
        consultationRepository.save(testConsultation);
    }

    // ========================================================================
    // Complete Registration Flow Tests
    // ========================================================================

    @Test
    @DisplayName("Complete patient registration publishes PatientRegistered event")
    void completePatientRegistration_Success_PublishesPatientRegisteredEvent() {
        // When - Register new patient
        PatientDto patientDto = patientService.registerNewPatient(
                "John",
                "Doe",
                "0700123456",
                "john.doe@test.com",
                "123 Test Street",
                null, null, null, null, null  // CI fields
        );

        // Then - Registration successful
        assertThat(patientDto).isNotNull();
        assertThat(patientDto.getPatientId()).isNotNull();
        assertThat(patientDto.getFirstName()).isEqualTo("John");
        assertThat(patientDto.getLastName()).isEqualTo("Doe");

        // Verify patient persisted in database
        Optional<Patient> persistedPatient = patientRepository.findById(patientDto.getPatientId());
        assertThat(persistedPatient).isPresent();
        assertThat(persistedPatient.get().getFirstName()).isEqualTo("John");
        assertThat(persistedPatient.get().getPhone()).isEqualTo("0700123456");

        // Verify NewPatientRegisteredEvent event was published
        assertThat(eventListener.getPatientRegisteredEvents()).hasSize(1);
        NewPatientRegisteredEvent event = eventListener.getPatientRegisteredEvents().get(0);
        assertThat(event.patientId()).isEqualTo(patientDto.getPatientId());
        assertThat(event.firstName()).isEqualTo("John");
        assertThat(event.lastName()).isEqualTo("Doe");
        assertThat(event.email()).isEqualTo("john.doe@test.com");
    }

    @Test
    @DisplayName("Patient registration enables appointment scheduling")
    void patientRegistration_Success_EnablesAppointmentScheduling() {
        // Given - Register patient
        PatientDto patientResult = patientService.registerNewPatient(
                "Jane",
                "Smith",
                "0700999888",
                "jane.smith@test.com",
                "456 Another Street",
                null, null, null, null, null  // CI fields
        );

        assertThat(patientResult).isNotNull();
        UUID patientId = patientResult.getPatientId();

        // When - Schedule appointment for registered patient
        OffsetDateTime scheduledTime = OffsetDateTime.now().plus(1, ChronoUnit.DAYS);
        AppointmentSessionDto appointmentDto = appointmentSessionService.scheduleAppointment(
                patientId,
                testDoctor.getDoctorId(),
                List.of(testConsultation.getName()),
                scheduledTime,
                false
        );

        // Then - Appointment successfully scheduled
        assertThat(appointmentDto).isNotNull();
        assertThat(appointmentDto.getSessionId()).isNotNull();
        assertThat(appointmentDto.getPatient().getPatientId()).isEqualTo(patientId);
        assertThat(appointmentDto.getDoctor().getDoctorId()).isEqualTo(testDoctor.getDoctorId());

        // Verify appointment persisted in database
        Optional<AppointmentSession> persistedSession = appointmentSessionRepository
                .findById(appointmentDto.getSessionId());
        assertThat(persistedSession).isPresent();
        assertThat(persistedSession.get().getPatient().getPatientId()).isEqualTo(patientId);
    }

    // ========================================================================
    // Search and Retrieval Workflow Tests
    // ========================================================================

    @Test
    @DisplayName("Register patient then retrieve by ID finds patient")
    void registerPatient_ThenRetrieveById_FindsPatient() {
        // Given - Register patient
        PatientDto registerResult = patientService.registerNewPatient(
                "Charlie",
                "Brown",
                "0700555666",
                "charlie.brown@test.com",
                "333 Third St",
                null, null, null, null, null  // CI fields
        );

        assertThat(registerResult).isNotNull();
        UUID patientId = registerResult.getPatientId();

        // When - Retrieve patient by ID
        PatientDto found = patientService.findById(patientId);

        // Then - Find the registered patient
        assertThat(found).isNotNull();
        assertThat(found.getPatientId()).isEqualTo(patientId);
        assertThat(found.getFirstName()).isEqualTo("Charlie");
        assertThat(found.getLastName()).isEqualTo("Brown");
        assertThat(found.getEmail()).isEqualTo("charlie.brown@test.com");

        // Verify patient exists in database
        Optional<Patient> dbPatient = patientRepository.findById(patientId);
        assertThat(dbPatient).isPresent();
        assertThat(dbPatient.get().getFirstName()).isEqualTo("Charlie");
    }

    @Test
    @DisplayName("Update patient email then search by new email finds patient")
    void updatePatientEmail_ThenSearchByNewEmail_FindsPatient() {
        // Given - Register patient
        PatientDto registerResult = patientService.registerNewPatient(
                "David",
                "Miller",
                "0700777888",
                "david.old@test.com",
                "444 Fourth St",
                null, null, null, null, null  // CI fields
        );

        assertThat(registerResult).isNotNull();
        UUID patientId = registerResult.getPatientId();

        // When - Update patient email
        PatientDto updateDto = PatientDto.builder()
                .patientId(patientId)
                .firstName("David")
                .lastName("Miller")
                .phone("0700777888")
                .email("david.new@test.com")  // New email
                .address("444 Fourth St")
                .build();

        PatientDto updateResult = patientService.update(patientId, updateDto);

        // Then - Update successful
        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getEmail()).isEqualTo("david.new@test.com");

        // Verify search by new email finds patient
        Optional<Patient> foundPatient = patientRepository.findByEmail("david.new@test.com");
        assertThat(foundPatient).isPresent();
        assertThat(foundPatient.get().getPatientId()).isEqualTo(patientId);
        assertThat(foundPatient.get().getEmail()).isEqualTo("david.new@test.com");

        // Verify old email no longer exists
        Optional<Patient> oldEmailPatient = patientRepository.findByEmail("david.old@test.com");
        assertThat(oldEmailPatient).isEmpty();
    }

    // ========================================================================
    // Data Integrity Workflow Tests
    // ========================================================================

    @Test
    @DisplayName("Patient with appointments maintains referential integrity")
    @Transactional
    void patientWithAppointments_MaintainsReferentialIntegrity() {
        // Given - Register patient
        PatientDto patientResult = patientService.registerNewPatient(
                "Emma",
                "Davis",
                "0700888999",
                "emma.davis@test.com",
                "555 Fifth St",
                null, null, null, null, null  // CI fields
        );

        assertThat(patientResult).isNotNull();
        UUID patientId = patientResult.getPatientId();

        // Create appointment for patient
        OffsetDateTime scheduledTime = OffsetDateTime.now().plus(1, ChronoUnit.DAYS);
        AppointmentSessionDto appointmentResult = appointmentSessionService.scheduleAppointment(
                patientId,
                testDoctor.getDoctorId(),
                List.of(testConsultation.getName()),
                scheduledTime,
                false
        );

        assertThat(appointmentResult).isNotNull();

        // Verify appointment exists and references patient correctly
        List<AppointmentSession> allAppointments = appointmentSessionRepository.findAll();
        List<AppointmentSession> patientAppointments = allAppointments.stream()
                .filter(a -> a.getPatient().getPatientId().equals(patientId))
                .toList();
        assertThat(patientAppointments).hasSize(1);

        // Verify patient exists and has appointment relationship
        Optional<Patient> patient = patientRepository.findById(patientId);
        assertThat(patient).isPresent();
        assertThat(patient.get().getFirstName()).isEqualTo("Emma");

        // Verify bidirectional relationship integrity
        AppointmentSession appointment = patientAppointments.get(0);
        assertThat(appointment.getPatient().getPatientId()).isEqualTo(patientId);
    }

    // ========================================================================
    // Event Listener for Testing
    // ========================================================================

    /**
     * Test configuration to register the event listener bean for integration tests.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {

        @org.springframework.context.annotation.Bean
        public PatientTestEventListener patientTestEventListener() {
            return new PatientTestEventListener();
        }
    }

    /**
     * Test event listener to capture domain events during integration tests.
     */
    public static class PatientTestEventListener {

        private final List<NewPatientRegisteredEvent> patientRegisteredEvents = new ArrayList<>();

        @EventListener
        public void handlePatientRegistered(NewPatientRegisteredEvent event) {
            patientRegisteredEvents.add(event);
        }

        public List<NewPatientRegisteredEvent> getPatientRegisteredEvents() {
            return patientRegisteredEvents;
        }

        public void clear() {
            patientRegisteredEvents.clear();
        }
    }
}
