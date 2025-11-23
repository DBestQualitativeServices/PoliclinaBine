package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.dto.AppointmentSessionFilterCriteria;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.AppointmentSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "Schedule a new appointment")
    public AppointmentSessionDto scheduleAppointment(
            @RequestParam UUID patientId,
            @RequestParam UUID doctorId,
            @RequestParam List<String> consultationNames,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime scheduledDateTime,
            @RequestParam(defaultValue = "false") boolean isEmergency
    ) {
        log.info("REST: Scheduling appointment for patient {} with doctor {} at {}",
                patientId, doctorId, scheduledDateTime);

        Result<AppointmentSessionDto> result = appointmentSessionService.scheduleAppointment(
                patientId,
                doctorId,
                consultationNames,
                scheduledDateTime,
                isEmergency
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{sessionId}")
    @StandardApiResponses
    @Operation(summary = "Get appointment by ID")
    public AppointmentSessionDto getAppointment(@PathVariable UUID sessionId) {
        log.info("REST: Getting appointment by ID: {}", sessionId);

        Result<AppointmentSessionDto> result = appointmentSessionService.findById(sessionId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Appointment", sessionId);
        }

        return result.getValue();
    }

    @GetMapping("/patient/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Get patient appointment history")
    public List<AppointmentSessionDto> getPatientHistory(@PathVariable UUID patientId) {
        log.info("REST: Getting appointment history for patient: {}", patientId);

        Result<List<AppointmentSessionDto>> result = appointmentSessionService.getPatientAppointmentHistory(patientId);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
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
    @Operation(summary = "Add consultation to existing appointment")
    public AppointmentSessionDto addConsultation(
            @PathVariable UUID sessionId,
            @RequestParam String consultationName
    ) {
        log.info("REST: Adding consultation {} to session {}", consultationName, sessionId);

        Result<AppointmentSessionDto> result = appointmentSessionService.addConsultationToSession(sessionId, consultationName);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PostMapping("/{sessionId}/start")
    @StandardApiResponses
    @Operation(summary = "Start appointment session")
    public AppointmentSessionDto startSession(@PathVariable UUID sessionId) {
        log.info("REST: Starting appointment session: {}", sessionId);

        Result<AppointmentSessionDto> result = appointmentSessionService.startSession(sessionId);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
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

        Result<AppointmentSessionDto> result = appointmentSessionService.addMedicalInformation(
                sessionId,
                diagnosisIds,
                freeTextDiagnosis,
                treatmentInstructions,
                notes
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
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

        Result<AppointmentSessionDto> result = appointmentSessionService.completeSession(
                sessionId,
                freeTextDiagnosis,
                treatmentInstructions,
                freeTextObservations
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
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

        Result<AppointmentSessionDto> result = appointmentSessionService.cancelAppointment(
                sessionId,
                reason,
                wasNoShow
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }
}
