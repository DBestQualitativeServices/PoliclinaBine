package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.service.MedicalFileAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medical-files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Medical File Access Control")
public class MedicalFileAccessController {

    private final MedicalFileAccessService medicalFileAccessService;

    @GetMapping("/access/doctor/{doctorId}/patient/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Check doctor access to patient records")
    public Boolean checkDoctorAccess(
            @PathVariable UUID doctorId,
            @PathVariable UUID patientId
    ) {
        log.info("REST: Checking doctor {} access to patient {} medical records",
                doctorId, patientId);

        Result<Boolean> result = medicalFileAccessService.canDoctorAccessMedicalData(
                doctorId,
                patientId
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/access/doctor/{doctorId}/patients")
    @StandardApiResponses
    @Operation(summary = "Get patients accessible to doctor")
    public List<PatientDto> getAccessiblePatients(@PathVariable UUID doctorId) {
        log.info("REST: Getting all patients accessible to doctor: {}", doctorId);

        Result<List<PatientDto>> result = medicalFileAccessService.getPatientsAccessibleToDoctor(doctorId);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/appointments/upcoming/doctor/{doctorId}/patient/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Check for upcoming appointments")
    public Boolean checkUpcomingAppointments(
            @PathVariable UUID doctorId,
            @PathVariable UUID patientId
    ) {
        log.info("REST: Checking upcoming appointments for doctor {} and patient {}",
                doctorId, patientId);

        Result<Boolean> result = medicalFileAccessService.hasUpcomingAppointments(
                doctorId,
                patientId
        );

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }
}
