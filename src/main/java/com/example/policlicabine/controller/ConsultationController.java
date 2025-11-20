package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.service.ConsultationService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for ConsultationType Management Operations.
 *
 * Provides CRUD endpoints for consultation type creation, retrieval,
 * update, and deletion. All operations use the ConsultationService
 * for business logic.
 */
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "ConsultationType Management",
        description = "APIs for managing consultation types, prices, and medical questionnaires"
)
public class ConsultationController {

    private final ConsultationService consultationService;

    @Operation(
            summary = "Create a new consultation type",
            description = """
                    Creates a new consultation type with price and description.

                    **Business Rules:**
                    - Name must be unique
                    - Price must be positive (BigDecimal for precision)
                    - Description is optional
                    - New consultations are active by default
                    - Publishes ConsultationCreated domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "ConsultationType created successfully",
                    content = @Content(schema = @Schema(implementation = ConsultationTypeDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or duplicate name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createConsultation(
            @Valid @RequestBody ConsultationTypeDto consultationDto,
            HttpServletRequest request
    ) {
        log.info("REST: Creating new consultation type: {}", consultationDto.getName());

        Result<ConsultationTypeDto> result = consultationService.createConsultation(
                consultationDto.getName(),
                consultationDto.getSpecialty(),
                consultationDto.getPrice(),
                consultationDto.getPriceCurrency(),
                consultationDto.getDurationMinutes(),
                consultationDto.getRequiresSurgeryRoom()
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
            summary = "Get consultation by ID",
            description = "Retrieves a consultation type with all associated questions"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "ConsultationType found",
                    content = @Content(schema = @Schema(implementation = ConsultationTypeDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "ConsultationType not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{consultationId}")
    public ResponseEntity<?> getConsultation(
            @Parameter(description = "ConsultationType UUID", required = true)
            @PathVariable UUID consultationId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting consultation by ID: {}", consultationId);

        Result<ConsultationTypeDto> result = consultationService.findById(consultationId);

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
            summary = "Get all consultations",
            description = "Retrieves a list of all consultation types in the system (active and inactive)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of consultations retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<ConsultationTypeDto>> getAllConsultations() {
        log.info("REST: Getting all consultations");

        Result<List<ConsultationTypeDto>> result = consultationService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @Operation(
            summary = "Update consultation information",
            description = """
                    Updates mutable fields of an existing consultation type.

                    **Mutable Fields:**
                    - Name
                    - Price
                    - Description
                    - Is active status

                    **Immutable Fields:**
                    - ConsultationType ID
                    - Created timestamp
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "ConsultationType updated successfully",
                    content = @Content(schema = @Schema(implementation = ConsultationTypeDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "ConsultationType not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{consultationId}")
    public ResponseEntity<?> updateConsultation(
            @Parameter(description = "ConsultationType UUID", required = true)
            @PathVariable UUID consultationId,
            @Valid @RequestBody ConsultationTypeDto consultationDto,
            HttpServletRequest request
    ) {
        log.info("REST: Updating consultation: {}", consultationId);

        Result<ConsultationTypeDto> result = consultationService.update(consultationId, consultationDto);

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
            summary = "Delete consultation",
            description = """
                    Permanently deletes a consultation type from the system.

                    **Warning:** This operation cannot be undone.
                    Use with caution and ensure proper authorization.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "ConsultationType deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "ConsultationType not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{consultationId}")
    public ResponseEntity<?> deleteConsultation(
            @Parameter(description = "ConsultationType UUID", required = true)
            @PathVariable UUID consultationId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting consultation: {}", consultationId);

        Result<Void> result = consultationService.deleteById(consultationId);

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

    @Operation(
            summary = "Update consultation price",
            description = """
                    Updates only the price of a consultation type.

                    **Business Rule:** Price must be positive
                    **Event:** Publishes ConsultationPriceUpdated event
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Price updated successfully",
                    content = @Content(schema = @Schema(implementation = ConsultationTypeDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "ConsultationType not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid price",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/{consultationId}/price")
    public ResponseEntity<?> updatePrice(
            @Parameter(description = "ConsultationType UUID", required = true)
            @PathVariable UUID consultationId,
            @Parameter(description = "New price", required = true)
            @RequestParam BigDecimal price,
            HttpServletRequest request
    ) {
        log.info("REST: Updating consultation price: {} to {}", consultationId, price);

        Result<ConsultationTypeDto> result = consultationService.updatePrice(consultationId, price);

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
            summary = "Deactivate consultation",
            description = """
                    Marks a consultation type as inactive.

                    Inactive consultations cannot be used for new appointments
                    but existing appointments remain valid.
                    """
    )
    @ApiResponse(responseCode = "200", description = "ConsultationType deactivated successfully")
    @PatchMapping("/{consultationId}/deactivate")
    public ResponseEntity<?> deactivateConsultation(
            @Parameter(description = "ConsultationType UUID", required = true)
            @PathVariable UUID consultationId,
            HttpServletRequest request
    ) {
        log.info("REST: Deactivating consultation: {}", consultationId);

        Result<ConsultationTypeDto> result = consultationService.deactivateConsultation(consultationId);

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
            summary = "Activate consultation",
            description = "Marks an inactive consultation type as active again"
    )
    @ApiResponse(responseCode = "200", description = "ConsultationType activated successfully")
    @PatchMapping("/{consultationId}/activate")
    public ResponseEntity<?> activateConsultation(
            @Parameter(description = "ConsultationType UUID", required = true)
            @PathVariable UUID consultationId,
            HttpServletRequest request
    ) {
        log.info("REST: Activating consultation: {}", consultationId);

        Result<ConsultationTypeDto> result = consultationService.activateConsultation(consultationId);

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
}
