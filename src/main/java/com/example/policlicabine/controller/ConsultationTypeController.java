package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.dto.ConsultationTypeFilterCriteria;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ConsultationType Management")
public class ConsultationTypeController {

    private final ConsultationService consultationService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Create a new consultation type")
    public ConsultationTypeDto createConsultation(@Valid @RequestBody ConsultationTypeDto consultationDto) {
        log.info("REST: Creating new consultation type: {}", consultationDto.getName());

        return consultationService.createConsultation(
                consultationDto.getName(),
                consultationDto.getSpecialty(),
                consultationDto.getPrice(),
                consultationDto.getPriceCurrency(),
                consultationDto.getDurationMinutes(),
                consultationDto.getRequiresSurgeryRoom()
        );
    }

    @GetMapping("/{consultationId}")
    @StandardApiResponses
    @Operation(summary = "Get consultation by ID")
    public ConsultationTypeDto getConsultation(@PathVariable UUID consultationId) {
        log.info("REST: Getting consultation by ID: {}", consultationId);
        return consultationService.findById(consultationId);
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all consultations")
    public List<ConsultationTypeDto> getAllConsultations() {
        log.info("REST: Getting all consultations");
        return consultationService.findAll();
    }

    @GetMapping("/search")
    @StandardApiResponses
    @Operation(summary = "Search and filter consultation types")
    public Page<ConsultationTypeDto> searchConsultationTypes(
            @ModelAttribute ConsultationTypeFilterCriteria criteria,
            @ParameterObject
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        log.info("REST: Searching consultation types with criteria: {}", criteria);
        return consultationService.search(criteria, pageable);
    }

    @PutMapping("/{consultationId}")
    @StandardApiResponses
    @Operation(summary = "Update consultation information")
    public ConsultationTypeDto updateConsultation(
            @PathVariable UUID consultationId,
            @Valid @RequestBody ConsultationTypeDto consultationDto
    ) {
        log.info("REST: Updating consultation: {}", consultationId);
        return consultationService.update(consultationId, consultationDto);
    }

    @DeleteMapping("/{consultationId}")
    @StandardApiResponses
    @Operation(summary = "Delete consultation")
    public void deleteConsultation(@PathVariable UUID consultationId) {
        log.info("REST: Deleting consultation: {}", consultationId);
        consultationService.deleteById(consultationId);
    }

    @PatchMapping("/{consultationId}/price")
    @StandardApiResponses
    @Operation(summary = "Update consultation price")
    public ConsultationTypeDto updatePrice(
            @PathVariable UUID consultationId,
            @RequestParam BigDecimal price
    ) {
        log.info("REST: Updating consultation price: {} to {}", consultationId, price);
        return consultationService.updatePrice(consultationId, price);
    }

    @PatchMapping("/{consultationId}/deactivate")
    @StandardApiResponses
    @Operation(summary = "Deactivate consultation")
    public ConsultationTypeDto deactivateConsultation(@PathVariable UUID consultationId) {
        log.info("REST: Deactivating consultation: {}", consultationId);
        return consultationService.deactivateConsultation(consultationId);
    }

    @PatchMapping("/{consultationId}/activate")
    @StandardApiResponses
    @Operation(summary = "Activate consultation")
    public ConsultationTypeDto activateConsultation(@PathVariable UUID consultationId) {
        log.info("REST: Activating consultation: {}", consultationId);
        return consultationService.activateConsultation(consultationId);
    }

    @PatchMapping("/{consultationId}/required-forms")
    @StandardApiResponses
    @Operation(summary = "Set required form templates for consultation type")
    public ConsultationTypeDto setRequiredFormTemplates(
            @PathVariable UUID consultationId,
            @RequestBody List<UUID> formTemplateIds
    ) {
        log.info("REST: Setting required form templates for consultation: {}", consultationId);
        return consultationService.setRequiredFormTemplates(consultationId, formTemplateIds);
    }

    @PatchMapping("/{consultationId}/consultation-form")
    @StandardApiResponses
    @Operation(summary = "Set main consultation form template")
    public ConsultationTypeDto setConsultationFormTemplate(
            @PathVariable UUID consultationId,
            @RequestBody(required = false) UUID formTemplateId
    ) {
        log.info("REST: Setting consultation form template for consultation: {}", consultationId);
        return consultationService.setConsultationFormTemplate(consultationId, formTemplateId);
    }

    @GetMapping("/{consultationId}/required-forms")
    @StandardApiResponses
    @Operation(summary = "Get required form templates for consultation type")
    public List<FormTemplateDto> getRequiredFormTemplates(@PathVariable UUID consultationId) {
        log.info("REST: Getting required form templates for consultation: {}", consultationId);
        return consultationService.getRequiredFormTemplates(consultationId);
    }

    @GetMapping("/{consultationId}/consultation-form")
    @StandardApiResponses
    @Operation(summary = "Get main consultation form template")
    public FormTemplateDto getConsultationFormTemplate(@PathVariable UUID consultationId) {
        log.info("REST: Getting consultation form template for consultation: {}", consultationId);
        return consultationService.getConsultationFormTemplate(consultationId);
    }
}
