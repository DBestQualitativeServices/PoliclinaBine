package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.entity.Payment;
import com.example.policlicabine.service.PaymentService;
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

import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Payment Processing Operations.
 *
 * Provides endpoints for payment processing, retrieval,
 * and payment history management.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Payment Management",
        description = "APIs for payment processing, tracking, and payment history"
)
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Process a payment",
            description = """
                    Processes a payment for one or more invoices.

                    **Business Rules:**
                    - At least one invoice is required
                    - All invoices must exist
                    - Payment amount must be positive
                    - Payment method is required (CASH, CARD, BANK_TRANSFER)
                    - Records payment date/time
                    - Publishes PaymentProcessed domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment processed successfully",
                    content = @Content(schema = @Schema(implementation = Payment.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payment data or invoices not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> processPayment(
            @Parameter(description = "Invoice IDs to pay", required = true)
            @RequestParam List<UUID> invoiceIds,
            @Parameter(description = "Payment amount", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Payment method (CASH, CARD, BANK_TRANSFER)", required = true)
            @RequestParam com.example.policlicabine.entity.enums.PaymentType paymentMethod,
            @Parameter(description = "User processing the payment", required = true)
            @RequestParam UUID generatedByUserId,
            @Parameter(description = "Transaction reference or notes")
            @RequestParam(required = false) String notes,
            HttpServletRequest request
    ) {
        log.info("REST: Processing payment for {} invoices, amount: {}, method: {}",
                invoiceIds.size(), amount, paymentMethod);

        Result<Payment> result = paymentService.processPayment(
                invoiceIds,
                amount,
                paymentMethod,
                generatedByUserId,
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
            summary = "Get payment by ID",
            description = "Retrieves a payment with all associated invoices and billings"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment found",
                    content = @Content(schema = @Schema(implementation = Payment.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(
            @Parameter(description = "Payment UUID", required = true)
            @PathVariable UUID paymentId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting payment by ID: {}", paymentId);

        Result<Payment> result = paymentService.getPaymentById(paymentId);

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
            summary = "Get all payments",
            description = "Retrieves a list of all payments in the system with invoice and billing details"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of payments retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        log.info("REST: Getting all payments");

        Result<List<Payment>> result = paymentService.getAllPayments();

        return ResponseEntity.ok(result.getValue());
    }
}
