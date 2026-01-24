package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.SessionBillingDto;
import com.example.policlicabine.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for billing management operations.
 *
 * <p>All endpoints return DTOs to maintain clean API contracts.</p>
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing Management", description = "APIs for managing session billing and discounts")
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/session/{sessionId}")
    @StandardApiResponses
    @Operation(summary = "Create session billing", description = "Create billing record for a completed appointment session")
    public SessionBillingDto createBilling(@PathVariable UUID sessionId) {
        log.info("REST: Creating billing for session: {}", sessionId);
        return billingService.createSessionBilling(sessionId);
    }

    @PostMapping("/session/{sessionId}/discount")
    @StandardApiResponses
    @Operation(summary = "Apply discount to billing", description = "Apply a discount to an existing session billing")
    public SessionBillingDto applyDiscount(
            @PathVariable UUID sessionId,
            @RequestParam UUID userId,
            @RequestParam BigDecimal discountAmount,
            @RequestParam(required = false) String reason
    ) {
        log.info("REST: Applying discount to session {} by user {}", sessionId, userId);
        return billingService.applyDiscount(sessionId, userId, discountAmount, reason);
    }

    @GetMapping("/session/{sessionId}")
    @StandardApiResponses
    @Operation(summary = "Get billing for session", description = "Retrieve billing information for a specific session")
    public SessionBillingDto getBilling(@PathVariable UUID sessionId) {
        log.info("REST: Getting billing for session: {}", sessionId);
        return billingService.getBillingForSession(sessionId);
    }

    @GetMapping("/session/{sessionId}/final-amount")
    @StandardApiResponses
    @Operation(summary = "Calculate final amount for session", description = "Calculate the final billable amount including discounts")
    public BigDecimal calculateFinalAmount(@PathVariable UUID sessionId) {
        log.info("REST: Calculating final amount for session: {}", sessionId);
        return billingService.calculateFinalAmount(sessionId);
    }
}
