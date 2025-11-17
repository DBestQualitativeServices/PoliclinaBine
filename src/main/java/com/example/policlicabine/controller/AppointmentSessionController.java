package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.service.AppointmentSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Appointment Session Management.
 *
 * Provides endpoints for appointment scheduling, session lifecycle management,
 * and medical information recording. This is the main aggregate root controller.
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Appointment Management",
        description = "APIs for appointment scheduling, session management, and medical documentation"
)
public class AppointmentSessionController {

    private final AppointmentSessionService appointmentSessionService;

    @Operation(
            summary = "Schedule a new appointment",
            description = """
                    Creates a new appointment session for a patient with a doctor.

                    **Business Rules:**
                    - Patient and doctor must exist
                    - Scheduled date/time must be in the future
                    - At least one consultation type is required
                    - Publishes AppointmentScheduled domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointment scheduled successfully",
                    content = @Content(schema = @Schema(implementation = AppointmentSessionDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> scheduleAppointment(
            @Parameter(description = "Patient UUID", required = true)
            @RequestParam UUID patientId,
            @Parameter(description = "Doctor UUID", required = true)
            @RequestParam UUID doctorId,
            @Parameter(description = "Consultation type names", required = true)
            @RequestParam List<String> consultationNames,
            @Parameter(description = "Scheduled date and time (ISO format: yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime scheduledDateTime,
            @Parameter(description = "Is emergency appointment")
            @RequestParam(defaultValue = "false") boolean isEmergency,
            HttpServletRequest request
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

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Get appointment by ID",
            description = "Retrieves a complete appointment session with all relationships loaded"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointment found",
                    content = @Content(schema = @Schema(implementation = AppointmentSessionDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getAppointment(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting appointment by ID: {}", sessionId);

        Result<AppointmentSessionDto> result = appointmentSessionService.findById(sessionId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(
                            HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Get patient appointment history",
            description = "Retrieves all appointments for a specific patient, ordered by scheduled date (most recent first)"
    )
    @ApiResponse(responseCode = "200", description = "Patient appointment history retrieved successfully")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientHistory(
            @Parameter(description = "Patient UUID", required = true)
            @PathVariable UUID patientId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting appointment history for patient: {}", patientId);

        Result<List<AppointmentSessionDto>> result = appointmentSessionService.getPatientAppointmentHistory(patientId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Add consultation to existing appointment",
            description = """
                    Adds an additional consultation type to an appointment session.

                    **Business Rules:**
                    - Appointment must be in SCHEDULED or IN_PROGRESS status
                    - Consultation must be active
                    """
    )
    @ApiResponse(responseCode = "200", description = "Consultation added successfully")
    @PatchMapping("/{sessionId}/consultations")
    public ResponseEntity<?> addConsultation(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            @Parameter(description = "Consultation type name", required = true)
            @RequestParam String consultationName,
            HttpServletRequest request
    ) {
        log.info("REST: Adding consultation {} to session {}", consultationName, sessionId);

        Result<AppointmentSessionDto> result = appointmentSessionService.addConsultationToSession(sessionId, consultationName);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Start appointment session",
            description = """
                    Marks an appointment as in progress.

                    **Business Rules:**
                    - Appointment must be in SCHEDULED status
                    - Publishes SessionStarted domain event
                    """
    )
    @ApiResponse(responseCode = "200", description = "Session started successfully")
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<?> startSession(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        log.info("REST: Starting appointment session: {}", sessionId);

        Result<AppointmentSessionDto> result = appointmentSessionService.startSession(sessionId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Add medical information to session",
            description = """
                    Records medical information during an appointment.

                    **Supports:**
                    - Adding diagnoses (ICD-10 codes)
                    - Recording free-text diagnosis notes
                    - General session notes
                    """
    )
    @ApiResponse(responseCode = "200", description = "Medical information added successfully")
    @PatchMapping("/{sessionId}/medical-info")
    public ResponseEntity<?> addMedicalInformation(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            @Parameter(description = "Diagnosis UUIDs")
            @RequestParam(required = false) List<UUID> diagnosisIds,
            @Parameter(description = "Free-text diagnosis notes")
            @RequestParam(required = false) String freeTextDiagnosis,
            @Parameter(description = "Treatment instructions")
            @RequestParam(required = false) String treatmentInstructions,
            @Parameter(description = "Additional session notes")
            @RequestParam(required = false) String notes,
            HttpServletRequest request
    ) {
        log.info("REST: Adding medical information to session: {}", sessionId);

        Result<AppointmentSessionDto> result = appointmentSessionService.addMedicalInformation(
                sessionId,
                diagnosisIds,
                freeTextDiagnosis,
                treatmentInstructions,
                notes
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Complete appointment session",
            description = """
                    Marks an appointment as completed with final documentation.

                    **Business Rules:**
                    - Session must be in IN_PROGRESS status
                    - Records actual end time
                    - Publishes SessionCompleted domain event
                    """
    )
    @ApiResponse(responseCode = "200", description = "Session completed successfully")
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<?> completeSession(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            @Parameter(description = "Free-text diagnosis summary")
            @RequestParam(required = false) String freeTextDiagnosis,
            @Parameter(description = "Treatment instructions")
            @RequestParam(required = false) String treatmentInstructions,
            @Parameter(description = "Free-text observations")
            @RequestParam(required = false) String freeTextObservations,
            HttpServletRequest request
    ) {
        log.info("REST: Completing appointment session: {}", sessionId);

        Result<AppointmentSessionDto> result = appointmentSessionService.completeSession(
                sessionId,
                freeTextDiagnosis,
                treatmentInstructions,
                freeTextObservations
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Cancel appointment",
            description = """
                    Cancels a scheduled or in-progress appointment.

                    **Business Rules:**
                    - Cannot cancel completed appointments
                    - Marks as no-show if patient didn't arrive
                    - Publishes AppointmentCancelled domain event
                    """
    )
    @ApiResponse(responseCode = "200", description = "Appointment cancelled successfully")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> cancelAppointment(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            @Parameter(description = "Cancellation reason")
            @RequestParam(required = false) String reason,
            @Parameter(description = "Was it a no-show?")
            @RequestParam(defaultValue = "false") boolean wasNoShow,
            HttpServletRequest request
    ) {
        log.info("REST: Cancelling appointment: {} (no-show: {})", sessionId, wasNoShow);

        Result<AppointmentSessionDto> result = appointmentSessionService.cancelAppointment(
                sessionId,
                reason,
                wasNoShow
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }
}
