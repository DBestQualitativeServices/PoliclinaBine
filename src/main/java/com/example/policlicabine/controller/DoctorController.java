package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.DoctorDto;
import com.example.policlicabine.dto.DoctorFilterCriteria;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> createDoctor(
            @Valid @RequestBody DoctorDto doctorDto,
            HttpServletRequest request
    ) {
        log.info("REST: Creating new doctor profile for user: {}",
                doctorDto.getUserId());

        Result<DoctorDto> result = doctorService.createDoctor(
                doctorDto.getUserId(),
                doctorDto.getFullName(),
                doctorDto.getSpecialties()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<?> getDoctor(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting doctor by ID: {}", doctorId);

        Result<DoctorDto> result = doctorService.findById(doctorId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(
                            HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @GetMapping
    public ResponseEntity<List<DoctorDto>> getAllDoctors() {
        log.info("REST: Getting all doctors");

        Result<List<DoctorDto>> result = doctorService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DoctorDto>> searchDoctors(
            @Parameter(description = "Filter criteria - all fields are optional flat query parameters")
            @ModelAttribute DoctorFilterCriteria criteria,
            @ParameterObject
            @Parameter(description = "Pagination and sorting parameters (page, size, sort)")
            @PageableDefault(size = 20, sort = "user.username")
            Pageable pageable
    ) {
        log.info("REST: Searching doctors with criteria: {} and pageable: {}", criteria, pageable);

        Page<DoctorDto> result = doctorService.search(criteria, pageable);

        log.info("REST: Doctor search returned {} results (page {}/{})",
                result.getNumberOfElements(),
                result.getNumber() + 1,
                result.getTotalPages());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{doctorId}")
    public ResponseEntity<?> updateDoctor(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorDto doctorDto,
            HttpServletRequest request
    ) {
        log.info("REST: Updating doctor: {}", doctorId);

        Result<DoctorDto> result = doctorService.update(doctorId, doctorDto);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            HttpStatus status = result.getErrorMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;

            return ResponseEntity
                    .status(status)
                    .body(ErrorResponse.of(
                            status.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @DeleteMapping("/{doctorId}")
    public ResponseEntity<?> deleteDoctor(
            @Parameter(description = "Doctor UUID", required = true)
            @PathVariable UUID doctorId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting doctor: {}", doctorId);

        Result<Void> result = doctorService.deleteById(doctorId);

        if (result.isSuccess()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(
                            HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }
}
