package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.DoctorDto;
import com.example.policlicabine.dto.DoctorFilterCriteria;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.DoctorService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Doctor Management",
        description = "APIs for doctor profile management, specialties, and availability"
)
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @StandardApiResponses
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create doctor profile (admin/manager only - for linking existing users)")
    public DoctorDto createDoctor(@Valid @RequestBody DoctorDto doctorDto) {
        log.info("REST: Creating new doctor profile for user: {}", doctorDto.getUserId());

        Result<DoctorDto> result = doctorService.createDoctor(
                doctorDto.getUserId(),
                doctorDto.getFullName(),
                doctorDto.getSpecialties()
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{doctorId}")
    @StandardApiResponses
    @Operation(summary = "Get doctor by ID")
    public DoctorDto getDoctor(@PathVariable UUID doctorId) {
        log.info("REST: Getting doctor by ID: {}", doctorId);

        Result<DoctorDto> result = doctorService.findById(doctorId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }

        return result.getValue();
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all doctors")
    public List<DoctorDto> getAllDoctors() {
        log.info("REST: Getting all doctors");
        return doctorService.findAll().getValue();
    }

    @GetMapping("/search")
    @StandardApiResponses
    @Operation(summary = "Search doctors")
    public Page<DoctorDto> searchDoctors(
            @ModelAttribute DoctorFilterCriteria criteria,
            @ParameterObject @PageableDefault(size = 20, sort = "user.username") Pageable pageable
    ) {
        log.info("REST: Searching doctors with criteria: {} and pageable: {}", criteria, pageable);
        return doctorService.search(criteria, pageable);
    }

    @PutMapping("/{doctorId}")
    @StandardApiResponses
    @Operation(summary = "Update doctor profile")
    public DoctorDto updateDoctor(
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorDto doctorDto
    ) {
        log.info("REST: Updating doctor: {}", doctorId);

        Result<DoctorDto> result = doctorService.update(doctorId, doctorDto);

        if (result.isFailure()) {
            if (result.getErrorMessage().contains("not found")) {
                throw new ResourceNotFoundException("Doctor", doctorId);
            }
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @DeleteMapping("/{doctorId}")
    @StandardApiResponses
    @Operation(summary = "Delete doctor profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDoctor(@PathVariable UUID doctorId) {
        log.info("REST: Deleting doctor: {}", doctorId);

        Result<Void> result = doctorService.deleteById(doctorId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }
    }
}
