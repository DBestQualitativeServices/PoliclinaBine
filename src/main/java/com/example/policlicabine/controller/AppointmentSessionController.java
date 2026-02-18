package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.dto.AppointmentSessionFilterCriteria;
import com.example.policlicabine.dto.FormReadinessDto;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.AppointmentSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.policlicabine.dto.BookingConflictErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Appointment Management")
public class AppointmentSessionController {

    private final AppointmentSessionService appointmentSessionService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Schedule a new appointment",
               description = "Creates a new appointment. Checks for booking conflicts unless forceOverride=true. " +
                           "Returns HTTP 409 if doctor has overlapping appointments (CANCELLED/NO_SHOW excluded).")
    @ApiResponse(responseCode = "409", description = "Booking conflict - Doctor has overlapping appointments",
                 content = @Content(schema = @Schema(implementation = BookingConflictErrorResponse.class)))
    public AppointmentSessionDto scheduleAppointment(
            @RequestParam UUID patientId,
            @RequestParam UUID doctorId,
            @RequestParam List<String> consultationNames,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime scheduledDateTime,
            @RequestParam(defaultValue = "false") boolean isEmergency,
            @RequestParam(defaultValue = "false") boolean forceOverride
    ) {
        log.info("REST: Scheduling appointment for patient {} with doctor {} at {} (forceOverride: {})",
                patientId, doctorId, scheduledDateTime, forceOverride);
        return appointmentSessionService.scheduleAppointment(patientId, doctorId, consultationNames,
            scheduledDateTime, isEmergency, forceOverride);
    }

    @PutMapping("/{sessionId}/reschedule")
    @StandardApiResponses
    @Operation(summary = "Reschedule an existing appointment",
               description = "Changes the scheduled time of an appointment. Checks for booking conflicts unless forceOverride=true. " +
                           "Cannot reschedule COMPLETED, CANCELLED, or NO_SHOW appointments.")
    @ApiResponse(responseCode = "409", description = "Booking conflict - Doctor has overlapping appointments",
                 content = @Content(schema = @Schema(implementation = BookingConflictErrorResponse.class)))
    public AppointmentSessionDto rescheduleAppointment(
            @PathVariable UUID sessionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime newScheduledDateTime,
            @RequestParam(defaultValue = "false") boolean forceOverride
    ) {
        log.info("REST: Rescheduling appointment {} to {} (forceOverride: {})",
                sessionId, newScheduledDateTime, forceOverride);
        return appointmentSessionService.rescheduleAppointment(sessionId, newScheduledDateTime, forceOverride);
    }

    @GetMapping("/{sessionId}")
    @StandardApiResponses
    @Operation(summary = "Get appointment by ID")
    public AppointmentSessionDto getAppointment(@PathVariable UUID sessionId) {
        log.info("REST: Getting appointment by ID: {}", sessionId);
        return appointmentSessionService.findById(sessionId);
    }

    @GetMapping("/patient/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Get patient appointment history")
    public List<AppointmentSessionDto> getPatientHistory(@PathVariable UUID patientId) {
        log.info("REST: Getting appointment history for patient: {}", patientId);
        return appointmentSessionService.getPatientAppointmentHistory(patientId);
    }

    @GetMapping("/search")
    @StandardApiResponses
    @Operation(summary = "Search and filter appointment sessions")
    public ResponseEntity<Page<AppointmentSessionDto>> searchAppointments(
            @ModelAttribute AppointmentSessionFilterCriteria criteria,
            @ParameterObject
            @PageableDefault(size = 20, sort = "scheduledDateTime", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("REST: Searching appointment sessions with criteria: {} and pageable: {}", criteria, pageable);

        Page<AppointmentSessionDto> result = appointmentSessionService.search(criteria, pageable);

        log.info("REST: Appointment session search returned {} results (page {}/{})",
                result.getNumberOfElements(),
                result.getNumber() + 1,
                result.getTotalPages());

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{sessionId}/consultations")
    @StandardApiResponses
    @Operation(summary = "Add consultation to existing appointment",
               description = "Adds a consultation to an appointment. Recalculates total duration and checks for conflicts unless forceOverride=true.")
    @ApiResponse(responseCode = "409", description = "Booking conflict - Doctor has overlapping appointments with new duration",
                 content = @Content(schema = @Schema(implementation = BookingConflictErrorResponse.class)))
    public AppointmentSessionDto addConsultation(
            @PathVariable UUID sessionId,
            @RequestParam String consultationName,
            @RequestParam(defaultValue = "false") boolean forceOverride
    ) {
        log.info("REST: Adding consultation {} to session {} (forceOverride: {})",
                consultationName, sessionId, forceOverride);
        return appointmentSessionService.addConsultationToSession(sessionId, consultationName, forceOverride);
    }

    @DeleteMapping("/{sessionId}/consultations/{consultationId}")
    @StandardApiResponses
    @Operation(summary = "Remove consultation type from existing appointment")
    public AppointmentSessionDto removeConsultation(
            @PathVariable UUID sessionId,
            @PathVariable UUID consultationId
    ) {
        log.info("REST: Removing consultation {} from session {}", consultationId, sessionId);
        return appointmentSessionService.removeConsultationFromSession(sessionId, consultationId);
    }

    @PostMapping("/{sessionId}/start")
    @StandardApiResponses
    @Operation(summary = "Start appointment session")
    public AppointmentSessionDto startSession(@PathVariable UUID sessionId) {
        log.info("REST: Starting appointment session: {}", sessionId);
        return appointmentSessionService.startSession(sessionId);
    }

    @PatchMapping("/{sessionId}/medical-info")
    @StandardApiResponses
    @Operation(summary = "Add medical information to session")
    public AppointmentSessionDto addMedicalInformation(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) List<UUID> diagnosisIds,
            @RequestParam(required = false) String freeTextDiagnosis,
            @RequestParam(required = false) String treatmentInstructions,
            @RequestParam(required = false) String notes
    ) {
        log.info("REST: Adding medical information to session: {}", sessionId);
        return appointmentSessionService.addMedicalInformation(sessionId, diagnosisIds, freeTextDiagnosis, treatmentInstructions, notes);
    }

    @PostMapping("/{sessionId}/complete")
    @StandardApiResponses
    @Operation(summary = "Complete appointment session")
    public AppointmentSessionDto completeSession(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String freeTextDiagnosis,
            @RequestParam(required = false) String treatmentInstructions,
            @RequestParam(required = false) String freeTextObservations
    ) {
        log.info("REST: Completing appointment session: {}", sessionId);
        return appointmentSessionService.completeSession(sessionId, freeTextDiagnosis, treatmentInstructions, freeTextObservations);
    }

    @DeleteMapping("/{sessionId}")
    @StandardApiResponses
    @Operation(summary = "Cancel appointment")
    public AppointmentSessionDto cancelAppointment(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "false") boolean wasNoShow
    ) {
        log.info("REST: Cancelling appointment: {} (no-show: {})", sessionId, wasNoShow);
        return appointmentSessionService.cancelAppointment(sessionId, reason, wasNoShow);
    }

    @GetMapping("/{sessionId}/required-forms")
    @StandardApiResponses
    @Operation(summary = "Get all required form templates for this appointment")
    public List<FormTemplateDto> getRequiredForms(@PathVariable UUID sessionId) {
        log.info("REST: Getting required forms for session: {}", sessionId);
        return appointmentSessionService.getRequiredFormsForSession(sessionId);
    }

    @GetMapping("/{sessionId}/missing-forms")
    @StandardApiResponses
    @Operation(summary = "Get form templates patient still needs to fill")
    public List<FormTemplateDto> getMissingForms(@PathVariable UUID sessionId) {
        log.info("REST: Getting missing forms for session: {}", sessionId);
        return appointmentSessionService.getMissingFormsForSession(sessionId);
    }

    /**
     * Checks form readiness for an appointment session (Forms v2 approach).
     * Returns comprehensive form requirement status with detailed breakdown.
     *
     * @param sessionId the appointment session ID
     * @return FormReadinessDto with all required forms and their statuses
     */
    @GetMapping("/{sessionId}/form-readiness")
    @StandardApiResponses
    @Operation(summary = "Check form readiness for appointment (v2)",
               description = "Returns detailed status of all required forms: VALID, MISSING, EXPIRED, PENDING_SIGNATURE")
    public FormReadinessDto getFormReadiness(@PathVariable UUID sessionId) {
        log.info("REST: Checking form readiness for session: {}", sessionId);
        return appointmentSessionService.checkFormReadiness(sessionId);
    }
}
