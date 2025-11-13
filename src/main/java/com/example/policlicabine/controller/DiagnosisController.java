package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.DiagnosisDto;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.service.DiagnosisService;
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
 * REST Controller for Diagnosis Management Operations.
 * <p>
 * Provides CRUD endpoints for ICD-10 diagnosis code management.
 * All operations use the DiagnosisService for business logic.
 */
@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Diagnosis Management",
        description = "APIs for managing ICD-10 diagnosis codes and medical conditions"
)
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @Operation(
            summary = "Create a new diagnosis",
            description = """
                    Creates a new ICD-10 diagnosis code entry.
                    
                    **Business Rules:**
                    - ICD-10 code must be unique
                    - Name is required
                    - Description is optional but recommended
                    - Publishes DiagnosisCreated domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Diagnosis created successfully",
                    content = @Content(schema = @Schema(implementation = DiagnosisDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or duplicate ICD-10 code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createDiagnosis(
            @Valid @RequestBody DiagnosisDto diagnosisDto,
            HttpServletRequest request
    ) {
        log.info("REST: Creating new diagnosis: {}",
                diagnosisDto.getIcd10Code());

        Result<DiagnosisDto> result = diagnosisService.createDiagnosis(
                diagnosisDto.getIcd10Code(),
                diagnosisDto.getIcd10Description()
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
            summary = "Get diagnosis by ID",
            description = "Retrieves a diagnosis entry by its unique UUID identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Diagnosis found",
                    content = @Content(schema = @Schema(implementation = DiagnosisDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diagnosis not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{diagnosisId}")
    public ResponseEntity<?> getDiagnosis(
            @Parameter(description = "Diagnosis UUID", required = true)
            @PathVariable UUID diagnosisId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting diagnosis by ID: {}", diagnosisId);

        Result<DiagnosisDto> result = diagnosisService.findById(diagnosisId);

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
            summary = "Get all diagnoses",
            description = "Retrieves a list of all ICD-10 diagnosis codes in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of diagnoses retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<DiagnosisDto>> getAllDiagnoses() {
        log.info("REST: Getting all diagnoses");

        Result<List<DiagnosisDto>> result = diagnosisService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @Operation(
            summary = "Update diagnosis information",
            description = """
                    Updates mutable fields of an existing diagnosis entry.
                    
                    **Mutable Fields:**
                    - ICD-10 code
                    - Name
                    - Description
                    
                    **Immutable Fields:**
                    - Diagnosis ID
                    - Created timestamp
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Diagnosis updated successfully",
                    content = @Content(schema = @Schema(implementation = DiagnosisDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diagnosis not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{diagnosisId}")
    public ResponseEntity<?> updateDiagnosis(
            @Parameter(description = "Diagnosis UUID", required = true)
            @PathVariable UUID diagnosisId,
            @Valid @RequestBody DiagnosisDto diagnosisDto,
            HttpServletRequest request
    ) {
        log.info("REST: Updating diagnosis: {}", diagnosisId);

        Result<DiagnosisDto> result = diagnosisService.update(diagnosisId, diagnosisDto);

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
            summary = "Delete diagnosis",
            description = """
                    Permanently deletes a diagnosis entry from the system.
                    
                    **Warning:** This operation cannot be undone.
                    Use with caution and ensure proper authorization.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Diagnosis deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Diagnosis not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{diagnosisId}")
    public ResponseEntity<?> deleteDiagnosis(
            @Parameter(description = "Diagnosis UUID", required = true)
            @PathVariable UUID diagnosisId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting diagnosis: {}", diagnosisId);

        Result<Void> result = diagnosisService.deleteById(diagnosisId);

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
