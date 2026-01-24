package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.PaymentDto;
import com.example.policlicabine.entity.enums.PaymentType;
import com.example.policlicabine.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for payment management operations.
 *
 * <p>All endpoints return DTOs to maintain clean API contracts.</p>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management", description = "APIs for processing and managing payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Process a payment", description = "Process a payment against one or more invoices")
    public PaymentDto processPayment(
            @RequestParam List<UUID> invoiceIds,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentType paymentMethod,
            @RequestParam UUID generatedByUserId,
            @RequestParam(required = false) String notes
    ) {
        log.info("REST: Processing payment for {} invoices, amount: {}, method: {}",
                invoiceIds.size(), amount, paymentMethod);
        return paymentService.processPayment(invoiceIds, amount, paymentMethod, generatedByUserId, notes);
    }

    @GetMapping("/{paymentId}")
    @StandardApiResponses
    @Operation(summary = "Get payment by ID", description = "Retrieve payment information by payment ID")
    public PaymentDto getPayment(@PathVariable UUID paymentId) {
        log.info("REST: Getting payment by ID: {}", paymentId);
        return paymentService.getPaymentById(paymentId);
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all payments", description = "Retrieve all payments in the system")
    public List<PaymentDto> getAllPayments() {
        log.info("REST: Getting all payments");
        return paymentService.getAllPayments();
    }
}
