package com.example.policlicabine.integration;

import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static com.example.policlicabine.util.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for booking overlap detection.
 * Tests complete end-to-end flows with real database (H2).
 * Spring Boot 4.0: Uses @AutoConfigureMockMvc with @SpringBootTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "test", roles = {"USER", "ADMIN"})
@DisplayName("Appointment Overlap Integration Tests")
@Tag("integration")
@Tag("slow")
class AppointmentOverlapIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentSessionRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private UserRepository userRepository;

    private Doctor testDoctor;
    private Patient testPatient1;
    private Patient testPatient2;
    private ConsultationType controlConsultation;
    private ConsultationType ecografie;

    @BeforeEach
    void setUp() {
        // Clean database
        appointmentRepository.deleteAll();
        consultationRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();

        // Setup test data
        User user = userRepository.save(user()
            .withUsername("dr.test")
            .build());

        testDoctor = doctorRepository.save(doctor()
            .withUser(user)
            .withFullName("Dr. Test")
            .build());

        testPatient1 = patientRepository.save(patient()
            .withName("Ion", "Popescu")
            .withPhone("+40721111111")
            .withEmail("ion.popescu@test.com")
            .build());

        testPatient2 = patientRepository.save(patient()
            .withName("Maria", "Ionescu")
            .withPhone("+40722222222")
            .withEmail("maria.ionescu@test.com")
            .build());

        controlConsultation = consultationRepository.save(consultation()
            .withName("Control dermatologic")
            .withDuration(30)
            .withPrice("150.00")
            .build());

        ecografie = consultationRepository.save(consultation()
            .withName("Ecografie")
            .withDuration(45)
            .withPrice("200.00")
            .build());
    }

    // ===================================================================
    // BASIC SCHEDULING TESTS
    // ===================================================================

    @Nested
    @DisplayName("Basic Scheduling Flows")
    class BasicSchedulingTests {

        @Test
        @DisplayName("Should successfully schedule appointment when no conflicts")
        void fullFlow_ScheduleAppointment_Success() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient1.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:00:00Z")
                    .param("forceOverride", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

            // Verify in database
            List<AppointmentSession> sessions = appointmentRepository.findAll();
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get(0).getTotalDurationMinutes()).isEqualTo(30);
            assertThat(sessions.get(0).getPatient().getPatientId())
                .isEqualTo(testPatient1.getPatientId());
        }

        @Test
        @DisplayName("Should calculate total duration for multiple consultations")
        void fullFlow_MultipleConsultations_CalculatesDuration() throws Exception {
            // When
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient1.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic", "Ecografie")
                    .param("scheduledDateTime", "2026-02-20T10:00:00Z"))
                .andExpect(status().isOk());

            // Then
            List<AppointmentSession> sessions = appointmentRepository.findAll();
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get(0).getTotalDurationMinutes())
                .isEqualTo(75); // 30 + 45
        }
    }

    // ===================================================================
    // OVERLAP DETECTION TESTS
    // ===================================================================

    @Nested
    @DisplayName("Overlap Detection Flows")
    class OverlapDetectionTests {

        @Test
        @DisplayName("Should detect overlap and return 409 CONFLICT")
        void fullFlow_OverlapDetection_Returns409() throws Exception {
            // Given - create first appointment
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .build());

            // When - try overlapping appointment
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:15:00Z")
                    .param("forceOverride", "false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.conflicts").isArray())
                .andExpect(jsonPath("$.conflicts", hasSize(1)))
                .andExpect(jsonPath("$.conflicts[0].patientName").value("Ion Popescu"))
                .andExpect(jsonPath("$.conflicts[0].status").value("SCHEDULED"));

            // Verify only 1 appointment in DB
            assertThat(appointmentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should detect partial overlap at start")
        void fullFlow_PartialOverlapAtStart_Returns409() throws Exception {
            // Given
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation) // 10:00-10:30
                .build());

            // When - overlaps at start (09:45-10:15)
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T09:45:00Z"))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should detect complete containment")
        void fullFlow_CompleteContainment_Returns409() throws Exception {
            // Given - longer appointment
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(ecografie) // 10:00-10:45
                .build());

            // When - shorter appointment inside (10:10-10:40)
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:10:00Z"))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should allow back-to-back appointments")
        void fullFlow_BackToBack_NoConflict() throws Exception {
            // Given
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation) // 10:00-10:30
                .build());

            // When - starts exactly when previous ends (10:30-11:00)
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:30:00Z"))
                .andExpect(status().isOk());

            // Then
            assertThat(appointmentRepository.count()).isEqualTo(2);
        }
    }

    // ===================================================================
    // STATUS EXCLUSION TESTS
    // ===================================================================

    @Nested
    @DisplayName("Status Exclusion Tests")
    class StatusExclusionTests {

        @Test
        @DisplayName("CANCELLED appointments should not block slots")
        void fullFlow_CancelledAppointment_DoesNotBlock() throws Exception {
            // Given
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .withStatus(SessionStatus.CANCELLED)
                .build());

            // When - same time
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:00:00Z"))
                .andExpect(status().isOk());

            assertThat(appointmentRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("NO_SHOW appointments should not block slots")
        void fullFlow_NoShowAppointment_DoesNotBlock() throws Exception {
            // Given
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .withStatus(SessionStatus.NO_SHOW)
                .build());

            // When
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:00:00Z"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SCHEDULED appointments should block slots")
        void fullFlow_ScheduledAppointment_Blocks() throws Exception {
            // Given
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .withStatus(SessionStatus.SCHEDULED)
                .build());

            // When
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:15:00Z"))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("IN_PROGRESS appointments should block slots")
        void fullFlow_InProgressAppointment_Blocks() throws Exception {
            // Given
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .withStatus(SessionStatus.IN_PROGRESS)
                .build());

            // When
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:15:00Z"))
                .andExpect(status().isConflict());
        }
    }

    // ===================================================================
    // FORCE OVERRIDE TESTS
    // ===================================================================

    @Nested
    @DisplayName("Force Override Tests")
    class ForceOverrideTests {

        @Test
        @DisplayName("forceOverride=true should bypass conflict detection")
        void fullFlow_ForceOverride_BypassesConflict() throws Exception {
            // Given
            appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .build());

            // When - overlapping time with forceOverride
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", testDoctor.getDoctorId().toString())
                    .param("consultationNames", "Ecografie")
                    .param("scheduledDateTime", "2026-02-20T10:15:00Z")
                    .param("forceOverride", "true"))
                .andExpect(status().isOk());

            // Then - both appointments saved
            assertThat(appointmentRepository.count()).isEqualTo(2);
        }
    }

    // ===================================================================
    // RESCHEDULE TESTS
    // ===================================================================

    @Nested
    @DisplayName("Reschedule Tests")
    class RescheduleTests {

        @Test
        @DisplayName("Should successfully reschedule when no conflicts")
        void fullFlow_Reschedule_NoConflict() throws Exception {
            // Given
            AppointmentSession session = appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .build());

            // When
            mockMvc.perform(put("/api/appointments/{sessionId}/reschedule", session.getSessionId())
                    .param("newScheduledDateTime", "2026-02-20T14:00:00Z"))
                .andExpect(status().isOk());

            // Then
            AppointmentSession updated = appointmentRepository
                .findById(session.getSessionId()).orElseThrow();
            assertThat(updated.getScheduledDateTime())
                .isEqualTo(OffsetDateTime.parse("2026-02-20T14:00:00Z"));
            assertThat(updated.getRescheduleCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should detect conflict during reschedule")
        void fullFlow_Reschedule_Conflict() throws Exception {
            // Given - two appointments
            AppointmentSession session1 = appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .build());

            appointmentRepository.save(appointment()
                .withPatient(testPatient2)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T14:00:00Z"))
                .build());

            // When - reschedule to conflicting time
            mockMvc.perform(put("/api/appointments/{sessionId}/reschedule", session1.getSessionId())
                    .param("newScheduledDateTime", "2026-02-20T14:15:00Z"))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should exclude self when checking conflicts")
        void fullFlow_Reschedule_ExcludesSelf() throws Exception {
            // Given
            AppointmentSession session = appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .build());

            // When - reschedule slightly (should not conflict with itself)
            mockMvc.perform(put("/api/appointments/{sessionId}/reschedule", session.getSessionId())
                    .param("newScheduledDateTime", "2026-02-20T10:05:00Z"))
                .andExpect(status().isOk());
        }
    }

    // ===================================================================
    // ADD CONSULTATION TESTS
    // ===================================================================

    @Nested
    @DisplayName("Add Consultation Tests")
    class AddConsultationTests {

        @Test
        @DisplayName("Should detect conflicts when adding consultation")
        void fullFlow_AddConsultation_DetectsConflict() throws Exception {
            // Given - session at 10:00 with 30min
            AppointmentSession session1 = appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .build());

            // Another at 10:45
            appointmentRepository.save(appointment()
                .withPatient(testPatient2)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:45:00Z"))
                .withConsultations(controlConsultation)
                .build());

            // When - add 45min consultation (total 75min, ends 11:15, conflicts!)
            mockMvc.perform(patch("/api/appointments/{sessionId}/consultations", session1.getSessionId())
                    .param("consultationName", "Ecografie"))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should successfully add consultation when no conflicts")
        void fullFlow_AddConsultation_Success() throws Exception {
            // Given
            AppointmentSession session = appointmentRepository.save(appointment()
                .withPatient(testPatient1)
                .withDoctor(testDoctor)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .withConsultations(controlConsultation)
                .build());

            // When
            mockMvc.perform(patch("/api/appointments/{sessionId}/consultations", session.getSessionId())
                    .param("consultationName", "Ecografie"))
                .andExpect(status().isOk());

            // Then
            AppointmentSession updated = appointmentRepository
                .findWithConsultationsBySessionId(session.getSessionId()).orElseThrow();
            assertThat(updated.getTotalDurationMinutes()).isEqualTo(75); // 30 + 45
            assertThat(updated.getConsultationTypes()).hasSize(2);
        }
    }

    // ===================================================================
    // DIFFERENT DOCTORS TESTS
    // ===================================================================

    @Nested
    @DisplayName("Different Doctors Tests")
    class DifferentDoctorsTests {

        @Test
        @DisplayName("Different doctors should not conflict")
        void fullFlow_DifferentDoctors_NoConflict() throws Exception {
            // Given - another doctor
            User user2 = userRepository.save(user()
                .withUsername("dr.other")
                .build());
            Doctor doctor2 = doctorRepository.save(doctor()
                .withUser(user2)
                .withFullName("Dr. Other")
                .build());

            // Create appointment for doctor 1
            appointmentRepository.save(appointment()
                .withDoctor(testDoctor)
                .withPatient(testPatient1)
                .at(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .build());

            // When - same time, different doctor
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", testPatient2.getPatientId().toString())
                    .param("doctorId", doctor2.getDoctorId().toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:00:00Z"))
                .andExpect(status().isOk());

            assertThat(appointmentRepository.count()).isEqualTo(2);
        }
    }
}
