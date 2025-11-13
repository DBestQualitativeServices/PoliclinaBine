package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.entity.SessionBilling;
import com.example.policlicabine.service.BillingService;
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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST Controller for Billing Operations.
 *
 * Provides endpoints for session billing calculation, discount application,
 * and billing information retrieval.
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Billing Management",
        description = "APIs for session billing calculation, discounts, and billing information"
)
public class BillingController {

    private final BillingService billingService;

    @Operation(
            summary = "Create session billing",
            description = """
                    Creates billing record for a completed appointment session.

                    **Business Rules:**
                    - Session must be completed
                    - Base amount calculated from consultation prices
                    - Discount starts at 0%
                    - Publishes SessionBillingCalculated domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Billing created successfully",
                    content = @Content(schema = @Schema(implementation = SessionBilling.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid session or session not completed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/session/{sessionId}")
    public ResponseEntity<?> createBilling(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        log.info("REST: Creating billing for session: {}", sessionId);

        Result<SessionBilling> result = billingService.createSessionBilling(sessionId);

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
            summary = "Apply discount to billing",
            description = """
                    Applies a percentage or fixed amount discount to a session billing.

                    **Business Rules:**
                    - Discount percentage: 0-100%
                    - Fixed amount: must not exceed base amount
                    - User ID tracks who applied the discount
                    - Publishes ManualDiscountApplied domain event
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Discount applied successfully",
                    content = @Content(schema = @Schema(implementation = SessionBilling.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid discount or billing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/session/{sessionId}/discount")
    public ResponseEntity<?> applyDiscount(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            @Parameter(description = "User applying discount", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Discount amount", required = true)
            @RequestParam BigDecimal discountAmount,
            @Parameter(description = "Discount reason")
            @RequestParam(required = false) String reason,
            HttpServletRequest request
    ) {
        log.info("REST: Applying discount to session {} by user {}", sessionId, userId);

        Result<SessionBilling> result = billingService.applyDiscount(
                sessionId,
                userId,
                discountAmount,
                reason
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
            summary = "Get billing for session",
            description = "Retrieves billing information for a specific appointment session"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Billing information retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SessionBilling.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Billing not found for session",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getBilling(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting billing for session: {}", sessionId);

        Result<SessionBilling> result = billingService.getBillingForSession(sessionId);

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
            summary = "Calculate final amount for session",
            description = """
                    Calculates the final payable amount for a session.

                    **Calculation:**
                    - Base amount (sum of consultation prices)
                    - Minus discounts applied
                    - Returns final amount to be paid
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Final amount calculated successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Billing not found for session",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/session/{sessionId}/final-amount")
    public ResponseEntity<?> calculateFinalAmount(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        log.info("REST: Calculating final amount for session: {}", sessionId);

        Result<BigDecimal> result = billingService.calculateFinalAmount(sessionId);

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
