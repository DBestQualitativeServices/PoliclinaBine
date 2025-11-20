package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.dto.PatientFilterCriteria;
import com.example.policlicabine.service.PatientService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Patient Management Operations.
 *
 * Provides CRUD endpoints for patient registration, retrieval,
 * update, and deletion. All operations use the PatientService
 * for business logic.
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Patient Management",
        description = "APIs for patient registration, profile management, and data retrieval"
)
public class PatientController {

    private final PatientService patientService;

    @Operation(
            summary = "Register a new patient",
            description = """
                    Creates a new patient record with personal information.

                    **Business Rules:**
                    - First name and last name are required
                    - Phone number is required
                    - Email must be valid format (if provided)
                    - Publishes PatientRegistered domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Patient registered successfully",
                    content = @Content(schema = @Schema(implementation = PatientDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> registerPatient(
            @Valid @RequestBody PatientDto patientDto,
            HttpServletRequest request
    ) {
        log.info("REST: Registering new patient: {} {}",
                patientDto.getFirstName(), patientDto.getLastName());

        Result<PatientDto> result = patientService.registerNewPatient(
                patientDto.getFirstName(),
                patientDto.getLastName(),
                patientDto.getPhone(),
                patientDto.getEmail(),
                patientDto.getAddress()
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
            summary = "Get patient by ID",
            description = "Retrieves a patient's full profile by their unique UUID identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Patient found",
                    content = @Content(schema = @Schema(implementation = PatientDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{patientId}")
    public ResponseEntity<?> getPatient(
            @Parameter(description = "Patient UUID", required = true)
            @PathVariable UUID patientId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting patient by ID: {}", patientId);

        Result<PatientDto> result = patientService.findById(patientId);

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
            summary = "Search patients with filters",
            description = """
                    Searches and filters patients with pagination and sorting support.

                    **Query Parameter Format:**
                    All parameters are flat query parameters (no nesting required).
                    Simply append filters and pagination params directly to the URL.

                    **Filter Options (all optional):**
                    - `firstName` - Partial match, case-insensitive (e.g., "joh" matches "John")
                    - `lastName` - Partial match, case-insensitive (e.g., "smi" matches "Smith")
                    - `phone` - Partial match (e.g., "0723" matches any phone containing "0723")
                    - `email` - Partial match, case-insensitive
                    - `registeredAfter` - Filter patients registered on or after this date (ISO 8601 format)
                    - `registeredBefore` - Filter patients registered on or before this date (ISO 8601 format)
                    - `hasConsent` - Boolean: true = with consent file, false = without consent file

                    **Pagination Parameters:**
                    - `page` - Page number (0-indexed, default: 0)
                    - `size` - Page size (default: 20, max: 100)
                    - `sort` - Sort criteria (e.g., "lastName,asc" or "registrationDate,desc")

                    **Examples:**
                    - `/api/patients/search?firstName=john&page=0&size=10`
                    - `/api/patients/search?hasConsent=true&sort=registrationDate,desc`
                    - `/api/patients/search?registeredAfter=2025-01-01T00:00:00Z&registeredBefore=2025-12-31T23:59:59Z`
                    - `/api/patients/search?firstName=mar&lastName=smith&phone=072&page=1&size=50&sort=lastName,asc`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Paginated patient results with metadata"
    )
    @GetMapping("/search")
    public ResponseEntity<Page<PatientDto>> searchPatients(
            @Parameter(description = "Filter criteria - all fields are optional flat query parameters")
            @ModelAttribute PatientFilterCriteria criteria,

            @Parameter(description = "Pagination and sorting parameters (page, size, sort)")
            @PageableDefault(size = 20, sort = "registrationDate")
            Pageable pageable
    ) {
        log.info("REST: Searching patients with criteria: {} and pageable: {}", criteria, pageable);

        Page<PatientDto> result = patientService.search(criteria, pageable);

        log.info("REST: Patient search returned {} results (page {}/{})",
                result.getNumberOfElements(),
                result.getNumber() + 1,
                result.getTotalPages());

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get all patients",
            description = "Retrieves a list of all registered patients in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of patients retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<PatientDto>> getAllPatients() {
        log.info("REST: Getting all patients");

        Result<List<PatientDto>> result = patientService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @Operation(
            summary = "Update patient information",
            description = """
                    Updates mutable fields of an existing patient record.

                    **Mutable Fields:**
                    - First name, last name
                    - Phone, email, address
                    - Consent status

                    **Immutable Fields:**
                    - Patient ID
                    - CNP (personal identification number)
                    - Date of birth
                    - Created timestamp
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Patient updated successfully",
                    content = @Content(schema = @Schema(implementation = PatientDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{patientId}")
    public ResponseEntity<?> updatePatient(
            @Parameter(description = "Patient UUID", required = true)
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientDto patientDto,
            HttpServletRequest request
    ) {
        log.info("REST: Updating patient: {}", patientId);

        Result<PatientDto> result = patientService.update(patientId, patientDto);

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
            summary = "Delete patient",
            description = """
                    Permanently deletes a patient record from the system.

                    **Warning:** This operation cannot be undone.
                    Use with caution and ensure proper authorization.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Patient deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{patientId}")
    public ResponseEntity<?> deletePatient(
            @Parameter(description = "Patient UUID", required = true)
            @PathVariable UUID patientId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting patient: {}", patientId);

        Result<Void> result = patientService.deleteById(patientId);

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
