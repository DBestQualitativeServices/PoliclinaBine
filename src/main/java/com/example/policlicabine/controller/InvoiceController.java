package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.dto.InvoiceDto;
import com.example.policlicabine.service.InvoiceService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Invoice Management Operations.
 *
 * Provides CRUD endpoints for invoice creation, retrieval,
 * update, and deletion, plus proforma to final conversion.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Invoice Management",
        description = "APIs for invoice management, including proforma and final invoices"
)
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(
            summary = "Create a new invoice",
            description = """
                    Creates a new invoice (proforma or final) with session billings.

                    **Business Rules:**
                    - Invoice number must be unique
                    - At least one session billing is required
                    - Can be proforma (draft) or final
                    - Total amount calculated from session billings
                    - Publishes InvoiceCreated domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invoice created successfully",
                    content = @Content(schema = @Schema(implementation = InvoiceDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or duplicate invoice number",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createInvoice(
            @Parameter(description = "Invoice number (must be unique)", required = true)
            @RequestParam String invoiceNumber,
            @Parameter(description = "Invoice date (ISO format: yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
            @Parameter(description = "Is this a proforma invoice?", required = true)
            @RequestParam boolean isProforma,
            @Parameter(description = "User generating the invoice", required = true)
            @RequestParam UUID generatedByUserId,
            @Parameter(description = "Session billing IDs to include", required = true)
            @RequestParam List<UUID> sessionBillingIds,
            HttpServletRequest request
    ) {
        log.info("REST: Creating invoice: {} (proforma: {})", invoiceNumber, isProforma);

        Result<InvoiceDto> result = invoiceService.createInvoice(
                invoiceNumber,
                invoiceDate,
                generatedByUserId,
                isProforma,
                sessionBillingIds
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
            summary = "Get invoice by ID",
            description = "Retrieves an invoice with all session billings and payment information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invoice found",
                    content = @Content(schema = @Schema(implementation = InvoiceDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{invoiceId}")
    public ResponseEntity<?> getInvoice(
            @Parameter(description = "Invoice UUID", required = true)
            @PathVariable UUID invoiceId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting invoice by ID: {}", invoiceId);

        Result<InvoiceDto> result = invoiceService.findById(invoiceId);

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
            summary = "Get invoice by number",
            description = "Retrieves an invoice by its unique invoice number"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invoice found",
                    content = @Content(schema = @Schema(implementation = InvoiceDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<?> getInvoiceByNumber(
            @Parameter(description = "Invoice number", required = true)
            @PathVariable String invoiceNumber,
            HttpServletRequest request
    ) {
        log.info("REST: Getting invoice by number: {}", invoiceNumber);

        Result<InvoiceDto> result = invoiceService.findInvoiceByNumber(invoiceNumber);

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
            summary = "Get all invoices",
            description = "Retrieves a list of all invoices in the system (proforma and final)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of invoices retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<InvoiceDto>> getAllInvoices() {
        log.info("REST: Getting all invoices");

        Result<List<InvoiceDto>> result = invoiceService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @Operation(
            summary = "Update invoice information",
            description = """
                    Updates mutable fields of an existing invoice.

                    **Mutable Fields:**
                    - Invoice number
                    - Invoice date
                    - Proforma status

                    **Immutable Fields:**
                    - Invoice ID
                    - Generated by user
                    - Session billings
                    - Created timestamp
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invoice updated successfully",
                    content = @Content(schema = @Schema(implementation = InvoiceDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{invoiceId}")
    public ResponseEntity<?> updateInvoice(
            @Parameter(description = "Invoice UUID", required = true)
            @PathVariable UUID invoiceId,
            @Valid @RequestBody InvoiceDto invoiceDto,
            HttpServletRequest request
    ) {
        log.info("REST: Updating invoice: {}", invoiceId);

        Result<InvoiceDto> result = invoiceService.update(invoiceId, invoiceDto);

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
            summary = "Delete invoice",
            description = """
                    Permanently deletes an invoice from the system.

                    **Warning:** This operation cannot be undone.
                    Use with caution and ensure proper authorization.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Invoice deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invoice not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<?> deleteInvoice(
            @Parameter(description = "Invoice UUID", required = true)
            @PathVariable UUID invoiceId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting invoice: {}", invoiceId);

        Result<Void> result = invoiceService.deleteById(invoiceId);

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
            summary = "Convert proforma to final invoice",
            description = """
                    Converts a proforma (draft) invoice to a final invoice.

                    **Business Rules:**
                    - Invoice must be proforma
                    - New invoice number must be unique
                    - Cannot convert already final invoices
                    - Publishes InvoiceConvertedToFinal domain event
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invoice converted successfully",
                    content = @Content(schema = @Schema(implementation = InvoiceDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid invoice or already final",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{invoiceId}/convert-to-final")
    public ResponseEntity<?> convertToFinal(
            @Parameter(description = "Invoice UUID", required = true)
            @PathVariable UUID invoiceId,
            @Parameter(description = "New final invoice number", required = true)
            @RequestParam String newInvoiceNumber,
            HttpServletRequest request
    ) {
        log.info("REST: Converting proforma invoice {} to final with number {}",
                invoiceId, newInvoiceNumber);

        Result<InvoiceDto> result = invoiceService.convertProformaToFinal(
                invoiceId,
                newInvoiceNumber
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
