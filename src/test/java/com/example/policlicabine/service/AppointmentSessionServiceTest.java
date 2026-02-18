package com.example.policlicabine.service;

import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.entity.AppointmentSession;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.exception.BookingConflictException;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.mapper.AppointmentSessionMapper;
import com.example.policlicabine.repository.AppointmentSessionRepository;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.repository.FormSubmissionRepository;
import com.example.policlicabine.specification.AppointmentSessionSpecificationBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.policlicabine.util.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentSessionService booking overlap detection.
 * Tests business logic in isolation using Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentSessionService Unit Tests")
@Tag("service")
@Tag("unit")
class AppointmentSessionServiceTest {

    @Mock
    private AppointmentSessionRepository appointmentRepository;

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private FormSubmissionRepository formSubmissionRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private ConsultationService consultationService;

    @Mock
    private DiagnosisService diagnosisService;

    @Mock
    private FormSubmissionService formSubmissionService;

    @Mock
    private FormReadinessService formReadinessService;

    @Mock
    private AppointmentSessionMapper appointmentMapper;

    @Mock
    private AppointmentSessionSpecificationBuilder specificationBuilder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AppointmentSessionService appointmentSessionService;

    private UUID doctorId;
    private UUID patientId;
    private OffsetDateTime baseTime;
    private Patient testPatient;
    private Doctor testDoctor;
    private ConsultationType testConsultation;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        baseTime = OffsetDateTime.parse("2026-02-20T10:00:00Z");

        testPatient = patient().withId(patientId).build();
        testDoctor = doctor().withId(doctorId).build();
        testConsultation = consultation()
            .withName(Fixtures.CONSULTATION_CONTROL)
            .withDuration(30)
            .build();
    }

    // ===================================================================
    // SCHEDULE APPOINTMENT TESTS
    // ===================================================================

    @Nested
    @DisplayName("scheduleAppointment() Tests")
    class ScheduleAppointmentTests {

        @Test
        @DisplayName("Should schedule appointment successfully when no conflicts")
        void scheduleAppointment_NoConflict_Success() {
            // Given
            List<String> consultationNames = List.of(Fixtures.CONSULTATION_CONTROL);

            doNothing().when(patientService).validatePatientExists(patientId);
            doNothing().when(doctorService).validateDoctorExists(doctorId);
            when(consultationService.getEntitiesByNames(consultationNames))
                .thenReturn(List.of(testConsultation));
            when(appointmentRepository.findOverlappingAppointments(
                eq(doctorId), any(), any(), anyList()))
                .thenReturn(Collections.emptyList()); // No conflicts
            when(entityManager.getReference(Patient.class, patientId))
                .thenReturn(testPatient);
            when(entityManager.getReference(Doctor.class, doctorId))
                .thenReturn(testDoctor);

            AppointmentSession savedSession = appointment()
                .withPatient(testPatient)
                .withDoctor(testDoctor)
                .at(baseTime)
                .withConsultations(testConsultation)
                .build();

            when(appointmentRepository.save(any(AppointmentSession.class)))
                .thenReturn(savedSession);

            AppointmentSessionDto expectedDto = AppointmentSessionDto.builder()
                .sessionId(savedSession.getSessionId())
                .build();
            when(appointmentMapper.toDto(savedSession))
                .thenReturn(expectedDto);

            // When
            AppointmentSessionDto result = appointmentSessionService.scheduleAppointment(
                patientId, doctorId, consultationNames, baseTime, false, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSessionId()).isEqualTo(savedSession.getSessionId());

            verify(appointmentRepository).findOverlappingAppointments(
                eq(doctorId), any(), any(), anyList());
            verify(appointmentRepository).save(any(AppointmentSession.class));
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw BookingConflictException when overlap detected")
        void scheduleAppointment_OverlapDetected_ThrowsException() {
            // Given
            List<String> consultationNames = List.of(Fixtures.CONSULTATION_CONTROL);

            doNothing().when(patientService).validatePatientExists(patientId);
            doNothing().when(doctorService).validateDoctorExists(doctorId);
            when(consultationService.getEntitiesByNames(consultationNames))
                .thenReturn(List.of(testConsultation));

            AppointmentSession conflictingSession = appointment()
                .withDoctor(testDoctor)
                .withPatient(patient().withName("Ion", "Popescu").build())
                .at(baseTime) // Same time!
                .withConsultations(testConsultation)
                .build();

            when(appointmentRepository.findOverlappingAppointments(
                eq(doctorId), any(), any(), anyList()))
                .thenReturn(List.of(conflictingSession)); // Conflict!

            // When & Then
            assertThatThrownBy(() -> appointmentSessionService.scheduleAppointment(
                patientId, doctorId, consultationNames, baseTime, false, false))
                .isInstanceOf(BookingConflictException.class)
                .satisfies(ex -> {
                    BookingConflictException bce = (BookingConflictException) ex;
                    assertThat(bce.getConflicts()).hasSize(1);
                    assertThat(bce.getConflicts().get(0).getSessionId())
                        .isEqualTo(conflictingSession.getSessionId());
                    assertThat(bce.getConflicts().get(0).getPatientName())
                        .isEqualTo("Ion Popescu");
                });

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should bypass conflict check when forceOverride=true")
        void scheduleAppointment_ForceOverride_Success() {
            // Given
            List<String> consultationNames = List.of(Fixtures.CONSULTATION_ECOGRAFIE);

            doNothing().when(patientService).validatePatientExists(patientId);
            doNothing().when(doctorService).validateDoctorExists(doctorId);

            ConsultationType ecografie = consultation()
                .withName(Fixtures.CONSULTATION_ECOGRAFIE)
                .withDuration(45)
                .build();

            when(consultationService.getEntitiesByNames(consultationNames))
                .thenReturn(List.of(ecografie));
            when(entityManager.getReference(Patient.class, patientId))
                .thenReturn(testPatient);
            when(entityManager.getReference(Doctor.class, doctorId))
                .thenReturn(testDoctor);

            AppointmentSession savedSession = appointment()
                .withConsultations(ecografie)
                .build();

            when(appointmentRepository.save(any(AppointmentSession.class)))
                .thenReturn(savedSession);
            when(appointmentMapper.toDto(any()))
                .thenReturn(AppointmentSessionDto.builder().build());

            // When
            AppointmentSessionDto result = appointmentSessionService.scheduleAppointment(
                patientId, doctorId, consultationNames, baseTime, false, true); // forceOverride!

            // Then
            assertThat(result).isNotNull();
            verify(appointmentRepository, never())
                .findOverlappingAppointments(any(), any(), any(), anyList());
            verify(appointmentRepository).save(any(AppointmentSession.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when no consultations provided")
        void scheduleAppointment_NoConsultations_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> appointmentSessionService.scheduleAppointment(
                patientId, doctorId, Collections.emptyList(), baseTime, false, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("At least one consultation is required");
        }

        @Test
        @DisplayName("Should throw BusinessException when consultation not found")
        void scheduleAppointment_ConsultationNotFound_ThrowsException() {
            // Given
            List<String> consultationNames = List.of("NonexistentConsultation");

            doNothing().when(patientService).validatePatientExists(patientId);
            doNothing().when(doctorService).validateDoctorExists(doctorId);
            when(consultationService.getEntitiesByNames(consultationNames))
                .thenReturn(Collections.emptyList()); // Not found!

            // When & Then
            assertThatThrownBy(() -> appointmentSessionService.scheduleAppointment(
                patientId, doctorId, consultationNames, baseTime, false, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Some consultations not found or inactive");
        }
    }

    // ===================================================================
    // RESCHEDULE APPOINTMENT TESTS
    // ===================================================================

    @Nested
    @DisplayName("rescheduleAppointment() Tests")
    class RescheduleAppointmentTests {

        @Test
        @DisplayName("Should exclude current session from conflict check")
        void rescheduleAppointment_ExcludesSelf_Success() {
            // Given
            UUID sessionId = UUID.randomUUID();
            OffsetDateTime newTime = baseTime.plusHours(1);

            AppointmentSession existingSession = appointment()
                .withId(sessionId)
                .withDoctor(testDoctor)
                .at(baseTime)
                .withConsultations(testConsultation)
                .build();

            when(appointmentRepository.findWithConsultationsBySessionId(sessionId))
                .thenReturn(Optional.of(existingSession));
            when(appointmentRepository.findOverlappingAppointmentsExcluding(
                eq(doctorId), eq(sessionId), any(), any(), anyList()))
                .thenReturn(Collections.emptyList());
            when(appointmentRepository.save(any()))
                .thenReturn(existingSession);
            when(appointmentMapper.toDto(any()))
                .thenReturn(AppointmentSessionDto.builder().build());

            // When
            appointmentSessionService.rescheduleAppointment(sessionId, newTime, false);

            // Then
            verify(appointmentRepository).findOverlappingAppointmentsExcluding(
                eq(doctorId), eq(sessionId), any(), any(), anyList());
            verify(appointmentRepository).save(argThat(session ->
                session.getScheduledDateTime().equals(newTime) &&
                session.getRescheduleCount() == 1
            ));
        }

        @Test
        @DisplayName("Should throw BookingConflictException when reschedule creates overlap")
        void rescheduleAppointment_Conflict_ThrowsException() {
            // Given
            UUID sessionId = UUID.randomUUID();
            OffsetDateTime newTime = baseTime.plusHours(1);

            AppointmentSession existingSession = appointment()
                .withId(sessionId)
                .withDoctor(testDoctor)
                .at(baseTime)
                .build();

            AppointmentSession conflictingSession = appointment()
                .withDoctor(testDoctor)
                .at(newTime) // Conflict at new time!
                .build();

            when(appointmentRepository.findWithConsultationsBySessionId(sessionId))
                .thenReturn(Optional.of(existingSession));
            when(appointmentRepository.findOverlappingAppointmentsExcluding(
                eq(doctorId), eq(sessionId), any(), any(), anyList()))
                .thenReturn(List.of(conflictingSession));

            // When & Then
            assertThatThrownBy(() -> appointmentSessionService
                .rescheduleAppointment(sessionId, newTime, false))
                .isInstanceOf(BookingConflictException.class);

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when rescheduling completed session")
        void rescheduleAppointment_CompletedSession_ThrowsException() {
            // Given
            UUID sessionId = UUID.randomUUID();
            OffsetDateTime newTime = baseTime.plusHours(1);

            AppointmentSession completedSession = appointment()
                .withId(sessionId)
                .withStatus(SessionStatus.COMPLETED)
                .build();

            when(appointmentRepository.findWithConsultationsBySessionId(sessionId))
                .thenReturn(Optional.of(completedSession));

            // When & Then
            assertThatThrownBy(() -> appointmentSessionService
                .rescheduleAppointment(sessionId, newTime, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot reschedule completed");
        }

        @Test
        @DisplayName("Should bypass conflict check when forceOverride=true during reschedule")
        void rescheduleAppointment_ForceOverride_Success() {
            // Given
            UUID sessionId = UUID.randomUUID();
            OffsetDateTime newTime = baseTime.plusHours(1);

            AppointmentSession existingSession = appointment()
                .withId(sessionId)
                .withDoctor(testDoctor)
                .build();

            when(appointmentRepository.findWithConsultationsBySessionId(sessionId))
                .thenReturn(Optional.of(existingSession));
            when(appointmentRepository.save(any()))
                .thenReturn(existingSession);
            when(appointmentMapper.toDto(any()))
                .thenReturn(AppointmentSessionDto.builder().build());

            // When
            appointmentSessionService.rescheduleAppointment(sessionId, newTime, true);

            // Then
            verify(appointmentRepository, never())
                .findOverlappingAppointmentsExcluding(any(), any(), any(), any(), anyList());
            verify(appointmentRepository).save(any());
        }
    }

    // ===================================================================
    // ADD CONSULTATION TESTS
    // ===================================================================

    @Nested
    @DisplayName("addConsultationToSession() Tests")
    class AddConsultationTests {

        @Test
        @DisplayName("Should recalculate duration and check conflicts when adding consultation")
        void addConsultation_RecalculatesDuration_ChecksConflicts() {
            // Given
            UUID sessionId = UUID.randomUUID();
            String newConsultationName = Fixtures.CONSULTATION_ECOGRAFIE;

            ConsultationType ecografie = consultation()
                .withName(Fixtures.CONSULTATION_ECOGRAFIE)
                .withDuration(45)
                .build();

            AppointmentSession existingSession = appointment()
                .withId(sessionId)
                .withDoctor(testDoctor)
                .at(baseTime)
                .withConsultations(testConsultation) // 30 min
                .build();

            when(appointmentRepository.findWithConsultationsBySessionId(sessionId))
                .thenReturn(Optional.of(existingSession));
            when(consultationService.getEntityByName(newConsultationName))
                .thenReturn(ecografie);
            when(appointmentRepository.findOverlappingAppointmentsExcluding(
                eq(doctorId), eq(sessionId), any(), any(), anyList()))
                .thenReturn(Collections.emptyList());
            when(appointmentRepository.save(any()))
                .thenReturn(existingSession);
            when(appointmentMapper.toDto(any()))
                .thenReturn(AppointmentSessionDto.builder().build());

            // When
            appointmentSessionService.addConsultationToSession(sessionId, newConsultationName, false);

            // Then
            verify(appointmentRepository).save(argThat(session ->
                session.getTotalDurationMinutes() == 75 && // 30 + 45
                session.getConsultationTypes().size() == 2
            ));
            verify(appointmentRepository).findOverlappingAppointmentsExcluding(
                eq(doctorId), eq(sessionId), any(), any(), anyList());
        }

        @Test
        @DisplayName("Should throw BookingConflictException when new duration creates overlap")
        void addConsultation_NewDurationCreatesConflict_ThrowsException() {
            // Given
            UUID sessionId = UUID.randomUUID();

            ConsultationType longConsultation = consultation()
                .withDuration(120) // Very long!
                .build();

            AppointmentSession existingSession = appointment()
                .withId(sessionId)
                .withDoctor(testDoctor)
                .at(baseTime)
                .build();

            AppointmentSession conflictingSession = appointment()
                .withDoctor(testDoctor)
                .at(baseTime.plusHours(1)) // Would now overlap with new duration
                .build();

            when(appointmentRepository.findWithConsultationsBySessionId(sessionId))
                .thenReturn(Optional.of(existingSession));
            when(consultationService.getEntityByName(any()))
                .thenReturn(longConsultation);
            when(appointmentRepository.findOverlappingAppointmentsExcluding(
                any(), any(), any(), any(), anyList()))
                .thenReturn(List.of(conflictingSession));

            // When & Then
            assertThatThrownBy(() -> appointmentSessionService
                .addConsultationToSession(sessionId, "Long Consultation", false))
                .isInstanceOf(BookingConflictException.class);
        }
    }

    // ===================================================================
    // OVERLAP LOGIC EDGE CASES (Parameterized Tests)
    // ===================================================================

    @Nested
    @DisplayName("Overlap Detection Logic Tests")
    class OverlapLogicTests {

        @ParameterizedTest
        @DisplayName("Should correctly detect overlaps based on time ranges")
        @CsvSource({
            "10:00, 10:30, 10:15, 10:45, true",  // Partial overlap
            "10:00, 10:30, 10:00, 10:30, true",  // Exact match
            "10:00, 11:00, 10:15, 10:45, true",  // Contained inside
            "10:00, 10:30, 10:30, 11:00, false", // Back-to-back (no overlap)
            "10:00, 10:30, 10:31, 11:00, false", // After (no overlap)
            "10:00, 10:30, 09:00, 09:30, false", // Before (no overlap)
            "10:00, 10:30, 09:45, 10:15, true",  // Overlap at start
            "10:00, 10:30, 10:15, 10:45, true"   // Overlap at end
        })
        void testOverlapDetection(String start1, String end1, String start2, String end2, boolean shouldOverlap) {
            // Parse times
            OffsetDateTime s1 = parseTime(start1);
            OffsetDateTime e1 = parseTime(end1);
            OffsetDateTime s2 = parseTime(start2);
            OffsetDateTime e2 = parseTime(end2);

            // Test overlap formula: s2 < e1 AND e2 > s1
            boolean actualOverlap = s2.isBefore(e1) && e2.isAfter(s1);

            assertThat(actualOverlap)
                .as("Overlap detection for [%s-%s] vs [%s-%s]", start1, end1, start2, end2)
                .isEqualTo(shouldOverlap);
        }

        private OffsetDateTime parseTime(String time) {
            String[] parts = time.split(":");
            return baseTime
                .withHour(Integer.parseInt(parts[0]))
                .withMinute(Integer.parseInt(parts[1]));
        }
    }
}
