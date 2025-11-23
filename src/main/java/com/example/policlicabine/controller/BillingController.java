package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.entity.SessionBilling;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing Management")
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/session/{sessionId}")
    @StandardApiResponses
    @Operation(summary = "Create session billing")
    public SessionBilling createBilling(@PathVariable UUID sessionId) {
        log.info("REST: Creating billing for session: {}", sessionId);

        Result<SessionBilling> result = billingService.createSessionBilling(sessionId);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PostMapping("/session/{sessionId}/discount")
    @StandardApiResponses
    @Operation(summary = "Apply discount to billing")
    public SessionBilling applyDiscount(
            @PathVariable UUID sessionId,
            @RequestParam UUID userId,
            @RequestParam BigDecimal discountAmount,
            @RequestParam(required = false) String reason
    ) {
        log.info("REST: Applying discount to session {} by user {}", sessionId, userId);

        Result<SessionBilling> result = billingService.applyDiscount(sessionId, userId, discountAmount, reason);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/session/{sessionId}")
    @StandardApiResponses
    @Operation(summary = "Get billing for session")
    public SessionBilling getBilling(@PathVariable UUID sessionId) {
        log.info("REST: Getting billing for session: {}", sessionId);

        Result<SessionBilling> result = billingService.getBillingForSession(sessionId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Billing for session", sessionId);
        }

        return result.getValue();
    }

    @GetMapping("/session/{sessionId}/final-amount")
    @StandardApiResponses
    @Operation(summary = "Calculate final amount for session")
    public BigDecimal calculateFinalAmount(@PathVariable UUID sessionId) {
        log.info("REST: Calculating final amount for session: {}", sessionId);

        Result<BigDecimal> result = billingService.calculateFinalAmount(sessionId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Billing for session", sessionId);
        }

        return result.getValue();
    }
}
