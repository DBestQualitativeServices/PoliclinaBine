package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.DoctorDto;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Doctor Management Operations.
 *
 * Provides CRUD endpoints for doctor profile creation, retrieval,
 * update, and deletion. All operations use the DoctorService
 * for business logic.
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Doctor Management",
        description = "APIs for doctor profile management, specialties, and availability"
)
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(
            summary = "Create a new doctor profile",
            description = """
                    Creates a new doctor profile linked to a user account.

                    **Business Rules:**
                    - User must exist before creating doctor profile
                    - Specialty and license number are required
                    - Publishes DoctorProfileCreated domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Doctor profile created successfully",
                    content = @Content(schema = @Schema(implementation = DoctorDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or user not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createDoctor(
            @Valid @RequestBody DoctorDto doctorDto,
            HttpServletRequest request
    ) {
        log.info("REST: Creating new doctor profile for user: {}",
                doctorDto.getUserId());

        Result<DoctorDto> result = doctorService.createDoctor(
                doctorDto.getUserId(),
                doctorDto.getSpecialties()
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
            summary = "Get doctor by ID",
            description = "Retrieves a doctor's full profile including user information and availability"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Doctor found",
                    content = @Content(schema = @Schema(implementation = DoctorDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{doctorId}")
    public ResponseEntity<?> getDoctor(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting doctor by ID: {}", doctorId);

        Result<DoctorDto> result = doctorService.findById(doctorId);

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
            summary = "Get all doctors",
            description = "Retrieves a list of all doctors in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of doctors retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<DoctorDto>> getAllDoctors() {
        log.info("REST: Getting all doctors");

        Result<List<DoctorDto>> result = doctorService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @Operation(
            summary = "Update doctor information",
            description = """
                    Updates mutable fields of an existing doctor profile.

                    **Mutable Fields:**
                    - Specialty
                    - License number
                    - Years of experience
                    - Bio

                    **Immutable Fields:**
                    - Doctor ID
                    - User relationship
                    - Created timestamp
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Doctor updated successfully",
                    content = @Content(schema = @Schema(implementation = DoctorDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{doctorId}")
    public ResponseEntity<?> updateDoctor(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorDto doctorDto,
            HttpServletRequest request
    ) {
        log.info("REST: Updating doctor: {}", doctorId);

        Result<DoctorDto> result = doctorService.update(doctorId, doctorDto);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            HttpStatus status = result.getErrorMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;

            return ResponseEntity
                    .status(status)
                    .body(ErrorResponse.of(
                            status.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Delete doctor",
            description = """
                    Permanently deletes a doctor profile from the system.

                    **Warning:** This operation cannot be undone.
                    Use with caution and ensure proper authorization.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Doctor deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{doctorId}")
    public ResponseEntity<?> deleteDoctor(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting doctor: {}", doctorId);

        Result<Void> result = doctorService.deleteById(doctorId);

        if (result.isSuccess()) {
            return ResponseEntity.noContent().build();
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
}
