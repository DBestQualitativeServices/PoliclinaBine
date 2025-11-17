package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.AppointmentSession;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing medical file access control.
 * Determines which doctors can access which patient records.
 *
 * Access rules:
 * - Doctors have access to patients with whom they have future appointments (30 days)
 * - Access is automatically granted when an appointment is scheduled
 * - Cancelled appointments don't grant access
 *
 * Architecture:
 * - Uses AppointmentSessionService for all appointment queries
 * - Read-only operations focused on access control
 * - Follows service-to-service communication pattern
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MedicalFileAccessService {

    // Service for appointment data access
    private final AppointmentSessionService appointmentSessionService;
    private final PatientMapper patientMapper;

    private static final int ACCESS_WINDOW_DAYS = 30;

    /**
     * Checks if a doctor can access a patient's medical records.
     * Access is granted if there are future appointments within the access window.
     *
     * Architecture notes:
     * - Uses AppointmentSessionService for appointment existence check
     *
     * @param doctorId Doctor identifier
     * @param patientId Patient identifier
     * @return Result containing Boolean access flag or error message
     */
    public Result<Boolean> canDoctorAccessMedicalData(UUID doctorId, UUID patientId) {
        try {
            if (doctorId == null) {
                return Result.failure("Doctor ID is required");
            }
            if (patientId == null) {
                return Result.failure("Patient ID is required");
            }

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime futureLimit = now.plusDays(ACCESS_WINDOW_DAYS);

            // Check for future appointments (not cancelled) via AppointmentSessionService
            boolean hasAccess = appointmentSessionService.hasAppointmentsInRange(
                doctorId, patientId, now, futureLimit, SessionStatus.CANCELLED);

            if (hasAccess) {
                log.debug("Doctor {} has access to patient {} medical records", doctorId, patientId);
            } else {
                log.debug("Doctor {} does NOT have access to patient {} medical records", doctorId, patientId);
            }

            return Result.success(hasAccess);

        } catch (Exception e) {
            log.error("Error checking doctor access", e);
            return Result.failure("Failed to check access: " + e.getMessage());
        }
    }

    /**
     * Retrieves all patients that a doctor can currently access.
     * Returns patients with future appointments within the access window.
     *
     * Architecture notes:
     * - Uses AppointmentSessionService to get appointments with patients loaded (EntityGraph)
     *
     * @param doctorId Doctor identifier
     * @return Result containing list of PatientDto or error message
     */
    public Result<List<PatientDto>> getPatientsAccessibleToDoctor(UUID doctorId) {
        try {
            if (doctorId == null) {
                return Result.failure("Doctor ID is required");
            }

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime futureLimit = now.plusDays(ACCESS_WINDOW_DAYS);

            // Get all future appointments (not cancelled) with patients loaded via AppointmentSessionService
            List<AppointmentSession> futureAppointments = appointmentSessionService
                .getAppointmentsInRangeWithPatient(doctorId, now, futureLimit, SessionStatus.CANCELLED);

            // Extract unique patients
            List<PatientDto> patients = futureAppointments.stream()
                .map(AppointmentSession::getPatient)
                .distinct()
                .map(patientMapper::toDto)
                .collect(Collectors.toList());

            log.info("Doctor {} has access to {} patients", doctorId, patients.size());

            return Result.success(patients);

        } catch (Exception e) {
            log.error("Error getting accessible patients", e);
            return Result.failure("Failed to get accessible patients: " + e.getMessage());
        }
    }

    /**
     * Checks if a doctor has any upcoming appointments with a specific patient.
     * Useful for UI to show if medical records are accessible.
     *
     * Architecture notes:
     * - Uses AppointmentSessionService for appointment existence check
     *
     * @param doctorId Doctor identifier
     * @param patientId Patient identifier
     * @return Result containing Boolean flag or error message
     */
    public Result<Boolean> hasUpcomingAppointments(UUID doctorId, UUID patientId) {
        try {
            if (doctorId == null) {
                return Result.failure("Doctor ID is required");
            }
            if (patientId == null) {
                return Result.failure("Patient ID is required");
            }

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime futureLimit = now.plusYears(1); // Check full year

            // Check via AppointmentSessionService
            boolean hasAppointments = appointmentSessionService.hasAppointmentsInRange(
                doctorId, patientId, now, futureLimit, SessionStatus.CANCELLED);

            return Result.success(hasAppointments);

        } catch (Exception e) {
            log.error("Error checking upcoming appointments", e);
            return Result.failure("Failed to check appointments: " + e.getMessage());
        }
    }
}
