package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.InvoiceDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice Management")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Create a new invoice")
    public InvoiceDto createInvoice(
            @RequestParam String invoiceNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime invoiceDate,
            @RequestParam boolean isProforma,
            @RequestParam UUID generatedByUserId,
            @RequestParam List<UUID> sessionBillingIds
    ) {
        log.info("REST: Creating invoice: {} (proforma: {})", invoiceNumber, isProforma);
        return invoiceService.createInvoice(invoiceNumber, invoiceDate, generatedByUserId, isProforma, sessionBillingIds);
    }

    @GetMapping("/{invoiceId}")
    @StandardApiResponses
    @Operation(summary = "Get invoice by ID")
    public InvoiceDto getInvoice(@PathVariable UUID invoiceId) {
        log.info("REST: Getting invoice by ID: {}", invoiceId);
        return invoiceService.findById(invoiceId);
    }

    @GetMapping("/number/{invoiceNumber}")
    @StandardApiResponses
    @Operation(summary = "Get invoice by number")
    public InvoiceDto getInvoiceByNumber(@PathVariable String invoiceNumber) {
        log.info("REST: Getting invoice by number: {}", invoiceNumber);
        return invoiceService.findInvoiceByNumber(invoiceNumber);
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all invoices")
    public List<InvoiceDto> getAllInvoices() {
        log.info("REST: Getting all invoices");
        return invoiceService.findAll();
    }

    @PutMapping("/{invoiceId}")
    @StandardApiResponses
    @Operation(summary = "Update invoice information")
    public InvoiceDto updateInvoice(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody InvoiceDto invoiceDto
    ) {
        log.info("REST: Updating invoice: {}", invoiceId);
        return invoiceService.update(invoiceId, invoiceDto);
    }

    @DeleteMapping("/{invoiceId}")
    @StandardApiResponses
    @Operation(summary = "Delete invoice")
    public void deleteInvoice(@PathVariable UUID invoiceId) {
        log.info("REST: Deleting invoice: {}", invoiceId);
        invoiceService.deleteById(invoiceId);
    }

    @PostMapping("/{invoiceId}/convert-to-final")
    @StandardApiResponses
    @Operation(summary = "Convert proforma to final invoice")
    public InvoiceDto convertToFinal(
            @PathVariable UUID invoiceId,
            @RequestParam String newInvoiceNumber
    ) {
        log.info("REST: Converting proforma invoice {} to final with number {}", invoiceId, newInvoiceNumber);
        return invoiceService.convertProformaToFinal(invoiceId, newInvoiceNumber);
    }
}
