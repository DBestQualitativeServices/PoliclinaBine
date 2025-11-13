package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.service.MedicalFileAccessService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Medical File Access Control.
 *
 * Provides endpoints for checking doctor access permissions to patient
 * medical records based on appointment history within a 30-day window.
 */
@RestController
@RequestMapping("/api/medical-files")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Medical File Access Control",
        description = "APIs for managing doctor access to patient medical records"
)
public class MedicalFileAccessController {

    private final MedicalFileAccessService medicalFileAccessService;

    @Operation(
            summary = "Check doctor access to patient records",
            description = """
                    Verifies if a doctor can access a patient's medical records.

                    **Access Rules:**
                    - Doctor must have appointments with patient within 30 days
                    - Appointments can be past (within 30 days) or upcoming
                    - Access is automatically managed based on appointment schedule
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Access check completed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid doctor or patient ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/access/doctor/{doctorId}/patient/{patientId}")
    public ResponseEntity<?> checkDoctorAccess(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            @Parameter(description = "Patient UUID", required = true)
            @PathVariable UUID patientId,
            HttpServletRequest request
    ) {
        log.info("REST: Checking doctor {} access to patient {} medical records",
                doctorId, patientId);

        Result<Boolean> result = medicalFileAccessService.canDoctorAccessMedicalData(
                doctorId,
                patientId
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
            summary = "Get patients accessible to doctor",
            description = """
                    Retrieves all patients whose medical records are accessible to a doctor.

                    Returns patients with appointments within the 30-day access window
                    (past or upcoming).
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of accessible patients retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid doctor ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/access/doctor/{doctorId}/patients")
    public ResponseEntity<?> getAccessiblePatients(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting all patients accessible to doctor: {}", doctorId);

        Result<List<PatientDto>> result = medicalFileAccessService.getPatientsAccessibleToDoctor(doctorId);

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
            summary = "Check for upcoming appointments",
            description = "Checks if a doctor has any upcoming appointments with a specific patient"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Appointment check completed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid doctor or patient ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/appointments/upcoming/doctor/{doctorId}/patient/{patientId}")
    public ResponseEntity<?> checkUpcomingAppointments(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            @Parameter(description = "Patient UUID", required = true)
            @PathVariable UUID patientId,
            HttpServletRequest request
    ) {
        log.info("REST: Checking upcoming appointments for doctor {} and patient {}",
                doctorId, patientId);

        Result<Boolean> result = medicalFileAccessService.hasUpcomingAppointments(
                doctorId,
                patientId
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
