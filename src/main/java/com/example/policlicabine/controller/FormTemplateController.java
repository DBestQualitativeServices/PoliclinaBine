package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.dto.FormTemplateFilterCriteria;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.service.FormTemplateService;
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
    public ResponseEntity<FormTemplateDto> createTemplate(@RequestBody CreateTemplateRequest request) {
        Result<FormTemplateDto> result = formTemplateService.createTemplate(
                request.code,
                request.name,
                request.structure,
                request.purpose,
                request.validityMonths,
                request.createdByUserId
        );

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormTemplateDto> updateTemplate(@PathVariable UUID id, @RequestBody FormTemplateDto dto) {
        Result<FormTemplateDto> result = formTemplateService.update(id, dto);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<FormTemplateDto> publishTemplate(@PathVariable UUID id) {
        Result<FormTemplateDto> result = formTemplateService.publishTemplate(id);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping
    public ResponseEntity<List<FormTemplateDto>> getAllActiveTemplates() {
        Result<List<FormTemplateDto>> result = formTemplateService.getActiveTemplates();

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<FormTemplateDto>> searchFormTemplates(
            @Parameter(description = "Filter criteria - all fields are optional flat query parameters")
            @ModelAttribute FormTemplateFilterCriteria criteria,
            @ParameterObject
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20, sort = "createdAt")
            Pageable pageable
    ) {
        Page<FormTemplateDto> result = formTemplateService.search(criteria, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/purpose/{purpose}")
    public ResponseEntity<List<FormTemplateDto>> getTemplatesByPurpose(@PathVariable FormPurpose purpose) {
        Result<List<FormTemplateDto>> result = formTemplateService.getTemplatesByPurpose(purpose);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/purpose/{purpose}/latest")
    public ResponseEntity<FormTemplateDto> getLatestTemplateByPurpose(@PathVariable FormPurpose purpose) {
        Result<FormTemplateDto> result = formTemplateService.getLatestTemplateByPurpose(purpose);

        if (result.isFailure()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormTemplateDto> getTemplateById(@PathVariable UUID id) {
        Result<FormTemplateDto> result = formTemplateService.findById(id);

        if (result.isFailure()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        Result<Void> result = formTemplateService.deleteById(id);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.noContent().build();
    }

    public record CreateTemplateRequest(
            String code,
            String name,
            FormStructure structure,
            FormPurpose purpose,
            Integer validityMonths,
            UUID createdByUserId
    ) {}
}
