package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.dto.PatientFilterCriteria;
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

        return patientService.registerNewPatient(
                patientDto.getFirstName(),
                patientDto.getLastName(),
                patientDto.getPhone(),
                patientDto.getEmail(),
                patientDto.getAddress(),
                patientDto.getCiSerie(),
                patientDto.getCiNumber(),
                patientDto.getCiEliberatDe(),
                patientDto.getCiDataEliberare(),
                patientDto.getCnp(),
                patientDto.getDataNastere(),
                patientDto.getSursa()
        );
    }

    @GetMapping("/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Get patient by ID")
    public PatientDto getPatient(@PathVariable UUID patientId) {
        log.info("REST: Getting patient by ID: {}", patientId);
        return patientService.findById(patientId);
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
        return patientService.findAll();
    }

    @PutMapping("/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Update patient information")
    public PatientDto updatePatient(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientDto patientDto
    ) {
        log.info("REST: Updating patient: {}", patientId);
        return patientService.update(patientId, patientDto);
    }

    @DeleteMapping("/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Delete patient")
    public void deletePatient(@PathVariable UUID patientId) {
        log.info("REST: Deleting patient: {}", patientId);
        patientService.deleteById(patientId);
    }

    @PostMapping("/{patientId}/tutor/{tutorPatientId}")
    @StandardApiResponses
    @Operation(summary = "Assign tutor to minor patient")
    public PatientDto assignTutor(
            @PathVariable UUID patientId,
            @PathVariable UUID tutorPatientId
    ) {
        log.info("REST: Assigning tutor {} to patient {}", tutorPatientId, patientId);
        return patientService.assignTutor(patientId, tutorPatientId);
    }

    @DeleteMapping("/{patientId}/tutor")
    @StandardApiResponses
    @Operation(summary = "Remove tutor from minor patient")
    public PatientDto removeTutor(@PathVariable UUID patientId) {
        log.info("REST: Removing tutor from patient {}", patientId);
        return patientService.removeTutor(patientId);
    }

    @GetMapping("/{tutorPatientId}/minors")
    @StandardApiResponses
    @Operation(summary = "Get all minors for a tutor")
    public List<PatientDto> getMinorsForTutor(@PathVariable UUID tutorPatientId) {
        log.info("REST: Getting minors for tutor {}", tutorPatientId);
        return patientService.getMinorsForTutor(tutorPatientId);
    }
}
