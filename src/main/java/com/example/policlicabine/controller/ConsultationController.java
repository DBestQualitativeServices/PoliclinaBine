package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ConsultationType Management")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Create a new consultation type")
    public ConsultationTypeDto createConsultation(@Valid @RequestBody ConsultationTypeDto consultationDto) {
        log.info("REST: Creating new consultation type: {}", consultationDto.getName());

        Result<ConsultationTypeDto> result = consultationService.createConsultation(
                consultationDto.getName(),
                consultationDto.getSpecialty(),
                consultationDto.getPrice(),
                consultationDto.getPriceCurrency(),
                consultationDto.getDurationMinutes(),
                consultationDto.getRequiresSurgeryRoom()
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{consultationId}")
    @StandardApiResponses
    @Operation(summary = "Get consultation by ID")
    public ConsultationTypeDto getConsultation(@PathVariable UUID consultationId) {
        log.info("REST: Getting consultation by ID: {}", consultationId);

        Result<ConsultationTypeDto> result = consultationService.findById(consultationId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("ConsultationType", consultationId);
        }

        return result.getValue();
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get all consultations")
    public List<ConsultationTypeDto> getAllConsultations() {
        log.info("REST: Getting all consultations");
        return consultationService.findAll().getValue();
    }

    @PutMapping("/{consultationId}")
    @StandardApiResponses
    @Operation(summary = "Update consultation information")
    public ConsultationTypeDto updateConsultation(
            @PathVariable UUID consultationId,
            @Valid @RequestBody ConsultationTypeDto consultationDto
    ) {
        log.info("REST: Updating consultation: {}", consultationId);

        Result<ConsultationTypeDto> result = consultationService.update(consultationId, consultationDto);

        if (result.isFailure()) {
            if (result.getErrorMessage().contains("not found")) {
                throw new ResourceNotFoundException("ConsultationType", consultationId);
            }
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @DeleteMapping("/{consultationId}")
    @StandardApiResponses
    @Operation(summary = "Delete consultation")
    public void deleteConsultation(@PathVariable UUID consultationId) {
        log.info("REST: Deleting consultation: {}", consultationId);

        Result<Void> result = consultationService.deleteById(consultationId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("ConsultationType", consultationId);
        }
    }

    @PatchMapping("/{consultationId}/price")
    @StandardApiResponses
    @Operation(summary = "Update consultation price")
    public ConsultationTypeDto updatePrice(
            @PathVariable UUID consultationId,
            @RequestParam BigDecimal price
    ) {
        log.info("REST: Updating consultation price: {} to {}", consultationId, price);

        Result<ConsultationTypeDto> result = consultationService.updatePrice(consultationId, price);

        if (result.isFailure()) {
            if (result.getErrorMessage().contains("not found")) {
                throw new ResourceNotFoundException("ConsultationType", consultationId);
            }
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @PatchMapping("/{consultationId}/deactivate")
    @StandardApiResponses
    @Operation(summary = "Deactivate consultation")
    public ConsultationTypeDto deactivateConsultation(@PathVariable UUID consultationId) {
        log.info("REST: Deactivating consultation: {}", consultationId);

        Result<ConsultationTypeDto> result = consultationService.deactivateConsultation(consultationId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("ConsultationType", consultationId);
        }

        return result.getValue();
    }

    @PatchMapping("/{consultationId}/activate")
    @StandardApiResponses
    @Operation(summary = "Activate consultation")
    public ConsultationTypeDto activateConsultation(@PathVariable UUID consultationId) {
        log.info("REST: Activating consultation: {}", consultationId);

        Result<ConsultationTypeDto> result = consultationService.activateConsultation(consultationId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("ConsultationType", consultationId);
        }

        return result.getValue();
    }
}
