package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.DiagnosisDto;
import com.example.policlicabine.service.DiagnosisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Diagnosis Management")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Create a new diagnosis")
    public DiagnosisDto createDiagnosis(@Valid @RequestBody DiagnosisDto diagnosisDto) {
        log.info("REST: Creating new diagnosis: {}", diagnosisDto.getIcd10Code());

        return diagnosisService.createDiagnosis(
                diagnosisDto.getIcd10Code(),
                diagnosisDto.getIcd10Description()
        );
    }

    @GetMapping("/{diagnosisId}")
    @StandardApiResponses
    @Operation(summary = "Get diagnosis by ID")
    public DiagnosisDto getDiagnosis(@PathVariable UUID diagnosisId) {
        log.info("REST: Getting diagnosis by ID: {}", diagnosisId);
        return diagnosisService.findById(diagnosisId);
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all diagnoses")
    public List<DiagnosisDto> getAllDiagnoses() {
        log.info("REST: Getting all diagnoses");
        return diagnosisService.findAll();
    }

    @PutMapping("/{diagnosisId}")
    @StandardApiResponses
    @Operation(summary = "Update diagnosis information")
    public DiagnosisDto updateDiagnosis(
            @PathVariable UUID diagnosisId,
            @Valid @RequestBody DiagnosisDto diagnosisDto
    ) {
        log.info("REST: Updating diagnosis: {}", diagnosisId);
        return diagnosisService.update(diagnosisId, diagnosisDto);
    }

    @DeleteMapping("/{diagnosisId}")
    @StandardApiResponses
    @Operation(summary = "Delete diagnosis")
    public void deleteDiagnosis(@PathVariable UUID diagnosisId) {
        log.info("REST: Deleting diagnosis: {}", diagnosisId);
        diagnosisService.deleteById(diagnosisId);
    }
}
