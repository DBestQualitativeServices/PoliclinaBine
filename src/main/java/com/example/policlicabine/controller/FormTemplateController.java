package com.example.policlicabine.controller;

import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.dto.FormTemplateFilterCriteria;
import com.example.policlicabine.entity.enums.OwnerType;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.service.FormTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/form-templates")
@RequiredArgsConstructor
public class FormTemplateController {

    private final FormTemplateService formTemplateService;

    @PostMapping
    public FormTemplateDto createTemplate(@RequestBody CreateTemplateRequest request) {
        return formTemplateService.createTemplate(
                request.name,
                request.structure,
                request.validityMonths,
                request.ownerType,
                request.createdByUserId
        );
    }

    @PutMapping("/{id}")
    public FormTemplateDto updateTemplate(@PathVariable UUID id, @RequestBody FormTemplateDto dto) {
        return formTemplateService.update(id, dto);
    }

    @PostMapping("/{id}/publish")
    public FormTemplateDto publishTemplate(@PathVariable UUID id) {
        return formTemplateService.publishTemplate(id);
    }

    @GetMapping
    public List<FormTemplateDto> getAllActiveTemplates() {
        return formTemplateService.getActiveTemplates();
    }

    @GetMapping("/search")
    public Page<FormTemplateDto> searchFormTemplates(
            @Parameter(description = "Filter criteria - all fields are optional flat query parameters")
            @ModelAttribute FormTemplateFilterCriteria criteria,
            @ParameterObject
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20, sort = "createdAt")
            Pageable pageable
    ) {
        return formTemplateService.search(criteria, pageable);
    }

    @GetMapping("/name/{name}")
    @StandardApiResponses
    @Operation(summary = "Get form template by name",
               description = "Optionally prefill form fields with patient data by providing patientId query parameter")
    public FormTemplateDto getTemplateByName(
            @PathVariable String name,
            @Parameter(description = "Optional patient ID to prefill form fields with patient data")
            @RequestParam(required = false) UUID patientId) {
        if (patientId != null) {
            return formTemplateService.getByNameWithPatientData(name, patientId);
        }
        return formTemplateService.getByName(name);
    }

    @GetMapping("/{id}")
    @StandardApiResponses
    @Operation(summary = "Get form template by ID",
               description = "Optionally prefill form fields with patient data by providing patientId query parameter")
    public FormTemplateDto getTemplateById(
            @PathVariable UUID id,
            @Parameter(description = "Optional patient ID to prefill form fields with patient data")
            @RequestParam(required = false) UUID patientId) {
        if (patientId != null) {
            return formTemplateService.findByIdWithPatientData(id, patientId);
        }
        return formTemplateService.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        formTemplateService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateTemplateRequest(
            String name,
            FormStructure structure,
            Integer validityMonths,
            OwnerType ownerType,
            UUID createdByUserId
    ) {}
}
