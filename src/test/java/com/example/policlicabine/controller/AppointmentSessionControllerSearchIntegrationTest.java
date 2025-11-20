package com.example.policlicabine.controller;

import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.dto.AppointmentSessionFilterCriteria;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.repository.*;
import com.example.policlicabine.service.AppointmentSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AppointmentSession search functionality with pagination.
 * <p>
 * Tests the complete flow from service to database query and response.
 * Uses an in-memory H2 database for testing.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AppointmentSession Search Integration Tests")
class AppointmentSessionControllerSearchIntegrationTest {

    @Autowired
    private AppointmentSessionService appointmentSessionService;

    @Autowired
    private AppointmentSessionRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    private Patient patient1;
    private Patient patient2;
    private Doctor doctor1;
    private Doctor doctor2;
    private ConsultationType consultation1;
    private ConsultationType consultation2;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        consultationRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users for doctors
        User user1 = User.builder()
                .username("dr.john")
                .password("password")
                .fullName("Dr. John Smith")
                .role(UserRole.DOCTOR)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        User user2 = User.builder()
                .username("dr.jane")
                .password("password")
                .fullName("Dr. Jane Doe")
                .role(UserRole.DOCTOR)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        // Create test doctors
        doctor1 = Doctor.builder()
                .user(user1)
                .build();

        doctor2 = Doctor.builder()
                .user(user2)
                .build();

        doctor1 = doctorRepository.save(doctor1);
        doctor2 = doctorRepository.save(doctor2);

        // Create test patients
        patient1 = Patient.builder()
                .firstName("John")
                .lastName("Patient")
                .phone("1234567890")
                .email("john.patient@example.com")
                .build();

        patient2 = Patient.builder()
                .firstName("Mary")
                .lastName("Johnson")
                .phone("0987654321")
                .email("mary.johnson@example.com")
                .build();

        patient1 = patientRepository.save(patient1);
        patient2 = patientRepository.save(patient2);

        // Create test consultations
        consultation1 = ConsultationType.builder()
                .name("General Checkup")
                .price(BigDecimal.valueOf(50.00))
                .isActive(true)
                .build();

        consultation2 = ConsultationType.builder()
                .name("X-Ray")
                .price(BigDecimal.valueOf(100.00))
                .isActive(true)
                .build();

        consultation1 = consultationRepository.save(consultation1);
        consultation2 = consultationRepository.save(consultation2);

        // Create test appointment sessions
        AppointmentSession session1 = AppointmentSession.builder()
                .patient(patient1)
                .doctor(doctor1)
                .scheduledDateTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10))
                .status(SessionStatus.COMPLETED)
                .isEmergency(false)
                .consultationTypes(Arrays.asList(consultation1))
                .build();

        AppointmentSession session2 = AppointmentSession.builder()
                .patient(patient2)
                .doctor(doctor1)
                .scheduledDateTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(5))
                .status(SessionStatus.SCHEDULED)
                .isEmergency(false)
                .consultationTypes(Arrays.asList(consultation2))
                .build();

        AppointmentSession session3 = AppointmentSession.builder()
                .patient(patient1)
                .doctor(doctor2)
                .scheduledDateTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2))
                .status(SessionStatus.IN_PROGRESS)
                .isEmergency(true)
                .consultationTypes(Arrays.asList(consultation1, consultation2))
                .build();

        appointmentRepository.save(session1);
        appointmentRepository.save(session2);
        appointmentRepository.save(session3);
    }

    @Test
    @DisplayName("Should search with patientName filter and return matching sessions")
    void searchAppointments_WithPatientNameFilter_ReturnsResults() {
        // Given
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder()
                .patientName("john")
                .build();
        Pageable pageable = PageRequest.of(0, 20, Sort.by("scheduledDateTime").descending());

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // 3 sessions: 2 for "John" Patient + 1 for Mary "Johnson"
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .allMatch(dto ->
                    dto.getPatient().getFirstName().toLowerCase().contains("john") ||
                    dto.getPatient().getLastName().toLowerCase().contains("john")
                );
    }

    @Test
    @DisplayName("Should search with doctorName filter and return matching sessions")
    void searchAppointments_WithDoctorNameFilter_ReturnsResults() {
        // Given
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder()
                .doctorName("smith")
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 2 sessions with Dr. John Smith
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should search with status=COMPLETED and return only completed sessions")
    void searchAppointments_WithStatusFilter_ReturnsOnlyCompletedSessions() {
        // Given
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder()
                .status(SessionStatus.COMPLETED)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).allMatch(dto -> dto.getStatus() == SessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should search with scheduledAfter filter and return future sessions")
    void searchAppointments_WithScheduledAfterFilter_ReturnsResults() {
        // Given
        OffsetDateTime cutoffDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(6);
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder()
                .scheduledAfter(cutoffDate)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 2 sessions after cutoff date
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should search with consultationNames filter and return matching sessions")
    void searchAppointments_WithConsultationNamesFilter_ReturnsResults() {
        // Given
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder()
                .consultationNames(Arrays.asList("X-Ray"))
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 2 sessions with X-Ray consultation
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should search with combined filters and return matching sessions")
    void searchAppointments_WithCombinedFilters_ReturnsResults() {
        // Given
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder()
                .patientName("john")
                .status(SessionStatus.IN_PROGRESS)
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1); // Only 1 session matches both criteria
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(result.getContent().get(0).getPatient().getFirstName()).isEqualToIgnoringCase("john");
    }

    @Test
    @DisplayName("Should search with pagination and return correct page")
    void searchAppointments_WithPagination_ReturnsCorrectPage() {
        // Given
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 2, Sort.by("scheduledDateTime").descending());

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // Page size is 2
        assertThat(result.getTotalElements()).isEqualTo(3); // Total 3 sessions
        assertThat(result.getTotalPages()).isEqualTo(2); // 3 sessions / 2 per page = 2 pages
        assertThat(result.getNumber()).isEqualTo(0); // First page
    }

    @Test
    @DisplayName("Should search with empty criteria and return all sessions")
    void searchAppointments_WithEmptyCriteria_ReturnsAllSessions() {
        // Given
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // All 3 sessions
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should search with no matching criteria and return empty result")
    void searchAppointments_WithNoMatches_ReturnsEmptyResult() {
        // Given
        AppointmentSessionFilterCriteria criteria = AppointmentSessionFilterCriteria.builder()
                .patientName("nonexistent")
                .build();
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }
}
