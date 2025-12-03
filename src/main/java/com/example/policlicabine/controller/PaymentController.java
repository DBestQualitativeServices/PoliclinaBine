package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.entity.Payment;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Process a payment")
    public Payment processPayment(
            @RequestParam List<UUID> invoiceIds,
            @RequestParam BigDecimal amount,
            @RequestParam com.example.policlicabine.entity.enums.PaymentType paymentMethod,
            @RequestParam UUID generatedByUserId,
            @RequestParam(required = false) String notes
    ) {
        log.info("REST: Processing payment for {} invoices, amount: {}, method: {}",
                invoiceIds.size(), amount, paymentMethod);
        return paymentService.processPayment(invoiceIds, amount, paymentMethod, generatedByUserId, notes);
    }

    @GetMapping("/{paymentId}")
    @StandardApiResponses
    @Operation(summary = "Get payment by ID")
    public Payment getPayment(@PathVariable UUID paymentId) {
        log.info("REST: Getting payment by ID: {}", paymentId);
        return paymentService.getPaymentById(paymentId);
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all payments")
    public List<Payment> getAllPayments() {
        log.info("REST: Getting all payments");
        return paymentService.getAllPayments();
    }
}
