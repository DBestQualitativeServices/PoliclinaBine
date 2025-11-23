package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
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

        Result<InvoiceDto> result = invoiceService.createInvoice(
                invoiceNumber,
                invoiceDate,
                generatedByUserId,
                isProforma,
                sessionBillingIds
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{invoiceId}")
    @StandardApiResponses
    @Operation(summary = "Get invoice by ID")
    public InvoiceDto getInvoice(@PathVariable UUID invoiceId) {
        log.info("REST: Getting invoice by ID: {}", invoiceId);

        Result<InvoiceDto> result = invoiceService.findById(invoiceId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Invoice", invoiceId);
        }

        return result.getValue();
    }

    @GetMapping("/number/{invoiceNumber}")
    @StandardApiResponses
    @Operation(summary = "Get invoice by number")
    public InvoiceDto getInvoiceByNumber(@PathVariable String invoiceNumber) {
        log.info("REST: Getting invoice by number: {}", invoiceNumber);

        Result<InvoiceDto> result = invoiceService.findInvoiceByNumber(invoiceNumber);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Invoice with number: " + invoiceNumber);
        }

        return result.getValue();
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all invoices")
    public List<InvoiceDto> getAllInvoices() {
        log.info("REST: Getting all invoices");
        return invoiceService.findAll().getValue();
    }

    @PutMapping("/{invoiceId}")
    @StandardApiResponses
    @Operation(summary = "Update invoice information")
    public InvoiceDto updateInvoice(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody InvoiceDto invoiceDto
    ) {
        log.info("REST: Updating invoice: {}", invoiceId);

        Result<InvoiceDto> result = invoiceService.update(invoiceId, invoiceDto);

        if (result.isFailure()) {
            if (result.getErrorMessage().contains("not found")) {
                throw new ResourceNotFoundException("Invoice", invoiceId);
            }
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @DeleteMapping("/{invoiceId}")
    @StandardApiResponses
    @Operation(summary = "Delete invoice")
    public void deleteInvoice(@PathVariable UUID invoiceId) {
        log.info("REST: Deleting invoice: {}", invoiceId);

        Result<Void> result = invoiceService.deleteById(invoiceId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Invoice", invoiceId);
        }
    }

    @PostMapping("/{invoiceId}/convert-to-final")
    @StandardApiResponses
    @Operation(summary = "Convert proforma to final invoice")
    public InvoiceDto convertToFinal(
            @PathVariable UUID invoiceId,
            @RequestParam String newInvoiceNumber
    ) {
        log.info("REST: Converting proforma invoice {} to final with number {}",
                invoiceId, newInvoiceNumber);

        Result<InvoiceDto> result = invoiceService.convertProformaToFinal(
                invoiceId,
                newInvoiceNumber
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }
}
