package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.DiagnosisDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
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

        Result<DiagnosisDto> result = diagnosisService.createDiagnosis(
                diagnosisDto.getIcd10Code(),
                diagnosisDto.getIcd10Description()
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{diagnosisId}")
    @StandardApiResponses
    @Operation(summary = "Get diagnosis by ID")
    public DiagnosisDto getDiagnosis(@PathVariable UUID diagnosisId) {
        log.info("REST: Getting diagnosis by ID: {}", diagnosisId);

        Result<DiagnosisDto> result = diagnosisService.findById(diagnosisId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Diagnosis", diagnosisId);
        }

        return result.getValue();
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all diagnoses")
    public List<DiagnosisDto> getAllDiagnoses() {
        log.info("REST: Getting all diagnoses");
        return diagnosisService.findAll().getValue();
    }

    @PutMapping("/{diagnosisId}")
    @StandardApiResponses
    @Operation(summary = "Update diagnosis information")
    public DiagnosisDto updateDiagnosis(
            @PathVariable UUID diagnosisId,
            @Valid @RequestBody DiagnosisDto diagnosisDto
    ) {
        log.info("REST: Updating diagnosis: {}", diagnosisId);

        Result<DiagnosisDto> result = diagnosisService.update(diagnosisId, diagnosisDto);

        if (result.isFailure()) {
            if (result.getErrorMessage().contains("not found")) {
                throw new ResourceNotFoundException("Diagnosis", diagnosisId);
            }
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @DeleteMapping("/{diagnosisId}")
    @StandardApiResponses
    @Operation(summary = "Delete diagnosis")
    public void deleteDiagnosis(@PathVariable UUID diagnosisId) {
        log.info("REST: Deleting diagnosis: {}", diagnosisId);

        Result<Void> result = diagnosisService.deleteById(diagnosisId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Diagnosis", diagnosisId);
        }
    }
}
