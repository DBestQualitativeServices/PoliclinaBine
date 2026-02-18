package com.example.policlicabine.controller;

import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.dto.BookingConflictDto;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.exception.BookingConflictException;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.GlobalExceptionHandler;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.AppointmentSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AppointmentSessionController booking endpoints.
 * Uses @WebMvcTest for fast, focused controller-layer testing.
 * Spring Boot 4.0: Excludes Security auto-configuration for web slice tests.
 */
@WebMvcTest(
    controllers = AppointmentSessionController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    }
)
@Import(GlobalExceptionHandler.class)
@DisplayName("AppointmentSessionController Tests")
@Tag("controller")
@Tag("unit")
class AppointmentSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentSessionService appointmentSessionService;

    @MockitoBean
    private com.example.policlicabine.security.JwtService jwtService;

    private UUID doctorId;
    private UUID patientId;
    private UUID sessionId;
    private OffsetDateTime scheduledTime;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        scheduledTime = OffsetDateTime.parse("2026-02-20T10:00:00Z");
    }

    // ===================================================================
    // SCHEDULE APPOINTMENT ENDPOINT TESTS
    // ===================================================================

    @Nested
    @DisplayName("POST /api/appointments - Schedule Appointment")
    class ScheduleAppointmentEndpointTests {

        @Test
        @DisplayName("Should return 200 OK when appointment scheduled successfully")
        void scheduleAppointment_NoConflict_Returns200() throws Exception {
            // Given
            AppointmentSessionDto expectedDto = AppointmentSessionDto.builder()
                .sessionId(sessionId)
                .scheduledDateTime(scheduledTime)
                .status(SessionStatus.SCHEDULED)
                .build();

            when(appointmentSessionService.scheduleAppointment(
                any(), any(), anyList(), any(), anyBoolean(), eq(false)))
                .thenReturn(expectedDto);

            // When & Then
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", patientId.toString())
                    .param("doctorId", doctorId.toString())
                    .param("consultationNames", "Control dermatologic")
                    .param("scheduledDateTime", "2026-02-20T10:00:00Z")
                    .param("forceOverride", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

            verify(appointmentSessionService).scheduleAppointment(
                eq(patientId), eq(doctorId), anyList(), any(), eq(false), eq(false));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when booking conflict detected")
        void scheduleAppointment_Conflict_Returns409() throws Exception {
            // Given
            BookingConflictDto conflict = BookingConflictDto.builder()
                .sessionId(UUID.randomUUID())
                .patientName("Ion Popescu")
                .startTime(OffsetDateTime.parse("2026-02-20T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-02-20T10:30:00Z"))
                .consultationNames(List.of("Control dermatologic"))
                .status(SessionStatus.SCHEDULED)
                .build();

            when(appointmentSessionService.scheduleAppointment(
                any(), any(), anyList(), any(), anyBoolean(), eq(false)))
                .thenThrow(BookingConflictException.of(List.of(conflict)));

            // When & Then
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", patientId.toString())
                    .param("doctorId", doctorId.toString())
                    .param("consultationNames", "Ecografie")
                    .param("scheduledDateTime", "2026-02-20T10:15:00Z")
                    .param("forceOverride", "false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.conflicts", hasSize(1)))
                .andExpect(jsonPath("$.conflicts[0].patientName").value("Ion Popescu"))
                .andExpect(jsonPath("$.conflicts[0].status").value("SCHEDULED"));
        }

        @Test
        @DisplayName("Should return 200 OK when forceOverride=true bypasses conflicts")
        void scheduleAppointment_ForceOverride_Returns200() throws Exception {
            // Given
            AppointmentSessionDto expectedDto = AppointmentSessionDto.builder()
                .sessionId(sessionId)
                .build();

            when(appointmentSessionService.scheduleAppointment(
                any(), any(), anyList(), any(), anyBoolean(), eq(true)))
                .thenReturn(expectedDto);

            // When & Then
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", patientId.toString())
                    .param("doctorId", doctorId.toString())
                    .param("consultationNames", "Ecografie")
                    .param("scheduledDateTime", "2026-02-20T10:15:00Z")
                    .param("forceOverride", "true"))
                .andExpect(status().isOk());

            verify(appointmentSessionService).scheduleAppointment(
                any(), any(), anyList(), any(), anyBoolean(), eq(true));
        }

        @Test
        @DisplayName("Should return 500 INTERNAL_ERROR when business exception occurs")
        void scheduleAppointment_BusinessException_Returns500() throws Exception {
            // Given
            when(appointmentSessionService.scheduleAppointment(
                any(), any(), anyList(), any(), anyBoolean(), anyBoolean()))
                .thenThrow(new BusinessException("At least one consultation is required"));

            // When & Then
            mockMvc.perform(post("/api/appointments")
                    .param("patientId", patientId.toString())
                    .param("doctorId", doctorId.toString())
                    .param("consultationNames", "")
                    .param("scheduledDateTime", "2026-02-20T10:00:00Z"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("At least one consultation is required")));
        }
    }

    // ===================================================================
    // RESCHEDULE APPOINTMENT ENDPOINT TESTS
    // ===================================================================

    @Nested
    @DisplayName("PUT /api/appointments/{sessionId}/reschedule - Reschedule Appointment")
    class RescheduleAppointmentEndpointTests {

        @Test
        @DisplayName("Should return 200 OK when reschedule successful")
        void rescheduleAppointment_NoConflict_Returns200() throws Exception {
            // Given
            OffsetDateTime newTime = OffsetDateTime.parse("2026-02-20T14:00:00Z");

            AppointmentSessionDto expectedDto = AppointmentSessionDto.builder()
                .sessionId(sessionId)
                .scheduledDateTime(newTime)
                .build();

            when(appointmentSessionService.rescheduleAppointment(
                eq(sessionId), any(), eq(false)))
                .thenReturn(expectedDto);

            // When & Then
            mockMvc.perform(put("/api/appointments/{sessionId}/reschedule", sessionId)
                    .param("newScheduledDateTime", "2026-02-20T14:00:00Z")
                    .param("forceOverride", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.scheduledDateTime").exists());
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when reschedule creates overlap")
        void rescheduleAppointment_Conflict_Returns409() throws Exception {
            // Given
            BookingConflictDto conflict = BookingConflictDto.builder()
                .sessionId(UUID.randomUUID())
                .patientName("Maria Ionescu")
                .startTime(OffsetDateTime.parse("2026-02-20T14:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-02-20T14:30:00Z"))
                .consultationNames(List.of("Ecografie"))
                .status(SessionStatus.SCHEDULED)
                .build();

            when(appointmentSessionService.rescheduleAppointment(
                eq(sessionId), any(), eq(false)))
                .thenThrow(BookingConflictException.of(List.of(conflict)));

            // When & Then
            mockMvc.perform(put("/api/appointments/{sessionId}/reschedule", sessionId)
                    .param("newScheduledDateTime", "2026-02-20T14:00:00Z")
                    .param("forceOverride", "false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts", hasSize(1)))
                .andExpect(jsonPath("$.conflicts[0].patientName").value("Maria Ionescu"));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when session does not exist")
        void rescheduleAppointment_SessionNotFound_Returns404() throws Exception {
            // Given
            when(appointmentSessionService.rescheduleAppointment(any(), any(), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Session", sessionId));

            // When & Then
            mockMvc.perform(put("/api/appointments/{sessionId}/reschedule", sessionId)
                    .param("newScheduledDateTime", "2026-02-20T14:00:00Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Session not found")));
        }

        @Test
        @DisplayName("Should return 200 OK when forceOverride=true during reschedule")
        void rescheduleAppointment_ForceOverride_Returns200() throws Exception {
            // Given
            AppointmentSessionDto expectedDto = AppointmentSessionDto.builder()
                .sessionId(sessionId)
                .build();

            when(appointmentSessionService.rescheduleAppointment(
                eq(sessionId), any(), eq(true)))
                .thenReturn(expectedDto);

            // When & Then
            mockMvc.perform(put("/api/appointments/{sessionId}/reschedule", sessionId)
                    .param("newScheduledDateTime", "2026-02-20T14:00:00Z")
                    .param("forceOverride", "true"))
                .andExpect(status().isOk());

            verify(appointmentSessionService).rescheduleAppointment(
                eq(sessionId), any(), eq(true));
        }
    }

    // ===================================================================
    // ADD CONSULTATION ENDPOINT TESTS
    // ===================================================================

    @Nested
    @DisplayName("PATCH /api/appointments/{sessionId}/consultations - Add Consultation")
    class AddConsultationEndpointTests {

        @Test
        @DisplayName("Should return 200 OK when consultation added successfully")
        void addConsultation_NoConflict_Returns200() throws Exception {
            // Given
            AppointmentSessionDto expectedDto = AppointmentSessionDto.builder()
                .sessionId(sessionId)
                .build();

            when(appointmentSessionService.addConsultationToSession(
                eq(sessionId), anyString(), eq(false)))
                .thenReturn(expectedDto);

            // When & Then
            mockMvc.perform(patch("/api/appointments/{sessionId}/consultations", sessionId)
                    .param("consultationName", "Ecografie")
                    .param("forceOverride", "false"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when new duration creates overlap")
        void addConsultation_Conflict_Returns409() throws Exception {
            // Given
            BookingConflictDto conflict = BookingConflictDto.builder()
                .sessionId(UUID.randomUUID())
                .patientName("Vasile Pop")
                .startTime(OffsetDateTime.parse("2026-02-20T10:30:00Z"))
                .endTime(OffsetDateTime.parse("2026-02-20T11:00:00Z"))
                .consultationNames(List.of("Tratament laser"))
                .status(SessionStatus.IN_PROGRESS)
                .build();

            when(appointmentSessionService.addConsultationToSession(
                eq(sessionId), anyString(), eq(false)))
                .thenThrow(BookingConflictException.of(List.of(conflict)));

            // When & Then
            mockMvc.perform(patch("/api/appointments/{sessionId}/consultations", sessionId)
                    .param("consultationName", "Ecografie")
                    .param("forceOverride", "false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts", hasSize(1)))
                .andExpect(jsonPath("$.conflicts[0].patientName").value("Vasile Pop"))
                .andExpect(jsonPath("$.conflicts[0].status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Should return 200 OK when forceOverride=true bypasses conflict")
        void addConsultation_ForceOverride_Returns200() throws Exception {
            // Given
            AppointmentSessionDto expectedDto = AppointmentSessionDto.builder()
                .sessionId(sessionId)
                .build();

            when(appointmentSessionService.addConsultationToSession(
                eq(sessionId), anyString(), eq(true)))
                .thenReturn(expectedDto);

            // When & Then
            mockMvc.perform(patch("/api/appointments/{sessionId}/consultations", sessionId)
                    .param("consultationName", "Ecografie")
                    .param("forceOverride", "true"))
                .andExpect(status().isOk());

            verify(appointmentSessionService).addConsultationToSession(
                eq(sessionId), anyString(), eq(true));
        }
    }

    // ===================================================================
    // GET APPOINTMENT BY ID TESTS
    // ===================================================================

    @Nested
    @DisplayName("GET /api/appointments/{sessionId} - Get Appointment By ID")
    class GetAppointmentEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with appointment details")
        void getAppointment_Exists_Returns200() throws Exception {
            // Given
            AppointmentSessionDto expectedDto = AppointmentSessionDto.builder()
                .sessionId(sessionId)
                .scheduledDateTime(scheduledTime)
                .status(SessionStatus.SCHEDULED)
                .build();

            when(appointmentSessionService.findById(sessionId))
                .thenReturn(expectedDto);

            // When & Then
            mockMvc.perform(get("/api/appointments/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        }

        @Test
        @DisplayName("Should return 404 NOT_FOUND when appointment does not exist")
        void getAppointment_NotFound_Returns404() throws Exception {
            // Given
            when(appointmentSessionService.findById(sessionId))
                .thenThrow(new ResourceNotFoundException("AppointmentSession", sessionId));

            // When & Then
            mockMvc.perform(get("/api/appointments/{sessionId}", sessionId))
                .andExpect(status().isNotFound());
        }
    }
}
