package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.dto.PatientFilterCriteria;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Management")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Register a new patient (admin/receptionist only - for walk-ins or linking)")
    public PatientDto registerPatient(@Valid @RequestBody PatientDto patientDto) {
        log.info("REST: Registering new patient: {} {}",
                patientDto.getFirstName(), patientDto.getLastName());

        Result<PatientDto> result = patientService.registerNewPatient(
                patientDto.getFirstName(),
                patientDto.getLastName(),
                patientDto.getPhone(),
                patientDto.getEmail(),
                patientDto.getAddress()
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Get patient by ID")
    public PatientDto getPatient(@PathVariable UUID patientId) {
        log.info("REST: Getting patient by ID: {}", patientId);

        Result<PatientDto> result = patientService.findById(patientId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Patient", patientId);
        }

        return result.getValue();
    }

    @GetMapping("/search")
    @StandardApiResponses
    @Operation(summary = "Search patients with filters")
    public Page<PatientDto> searchPatients(
            @ModelAttribute PatientFilterCriteria criteria,
            @ParameterObject @PageableDefault(size = 20, sort = "registrationDate") Pageable pageable
    ) {
        log.info("REST: Searching patients with criteria: {} and pageable: {}", criteria, pageable);

        Page<PatientDto> result = patientService.search(criteria, pageable);

        log.info("REST: Patient search returned {} results (page {}/{})",
                result.getNumberOfElements(),
                result.getNumber() + 1,
                result.getTotalPages());

        return result;
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all patients")
    public List<PatientDto> getAllPatients() {
        log.info("REST: Getting all patients");
        return patientService.findAll().getValue();
    }

    @PutMapping("/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Update patient information")
    public PatientDto updatePatient(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientDto patientDto
    ) {
        log.info("REST: Updating patient: {}", patientId);

        Result<PatientDto> result = patientService.update(patientId, patientDto);

        if (result.isFailure()) {
            if (result.getErrorMessage().contains("not found")) {
                throw new ResourceNotFoundException("Patient", patientId);
            }
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @DeleteMapping("/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Delete patient")
    public void deletePatient(@PathVariable UUID patientId) {
        log.info("REST: Deleting patient: {}", patientId);

        Result<Void> result = patientService.deleteById(patientId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
    }
}
