package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.event.*;
import com.example.policlicabine.mapper.AppointmentSessionMapper;
import com.example.policlicabine.repository.AppointmentSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing AppointmentSession entities.
 *
 * Architecture:
 * - Only uses AppointmentSessionRepository (single responsibility)
 * - Calls other services for validation and entity access (separation of concerns)
 * - Uses EntityGraph to prevent N+1 queries (performance optimization)
 * - Uses EntityManager.getReference() to avoid unnecessary DB hits
 * - Publishes domain events for cross-service communication (decoupling)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentSessionService {

    // Only our repository - single responsibility principle
    private final AppointmentSessionRepository appointmentRepository;

    // Services for validation and entity access - service-to-service communication
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final ConsultationService consultationService;
    private final DiagnosisService diagnosisService;

    // Mapper and event publisher
    private final AppointmentSessionMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    // EntityManager for creating entity references without DB hits
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Schedules a new appointment session.
     *
     * Architecture notes:
     * - Uses service validation for patient and doctor (Result pattern for error messages)
     * - Gets consultation entities via ConsultationService
     * - Uses EntityManager.getReference() for Patient/Doctor to avoid DB hits
     * - Publishes AppointmentScheduled event for decoupling
     *
     * @param patientId Patient identifier
     * @param doctorId Doctor identifier
     * @param consultationNames List of consultation names
     * @param scheduledDateTime When the appointment is scheduled
     * @param isEmergency Whether this is an emergency appointment
     * @return Result containing AppointmentSessionDto or error message
     */
    public Result<AppointmentSessionDto> scheduleAppointment(UUID patientId, UUID doctorId,
                                                           List<String> consultationNames,
                                                           OffsetDateTime scheduledDateTime,
                                                           boolean isEmergency) {
        try {
            // Input validation
            if (consultationNames == null || consultationNames.isEmpty()) {
                return Result.failure("At least one consultation is required");
            }
            if (scheduledDateTime == null) {
                return Result.failure("Scheduled date and time is required");
            }

            // Validate patient exists via PatientService (uses Result for clear error messages)
            Result<Void> patientCheck = patientService.validatePatientExists(patientId);
            if (patientCheck.isFailure()) {
                return Result.failure(patientCheck.getErrorMessage());
            }

            // Validate doctor exists via DoctorService
            Result<Void> doctorCheck = doctorService.validateDoctorExists(doctorId);
            if (doctorCheck.isFailure()) {
                return Result.failure(doctorCheck.getErrorMessage());
            }

            // Get consultation entities via ConsultationService (internal method, returns entities directly)
            List<Consultation> consultations = consultationService.getEntitiesByNames(consultationNames);
            if (consultations.size() != consultationNames.size()) {
                return Result.failure("Some consultations not found or inactive");
            }

            // Use EntityManager.getReference() to create entity proxies without DB hits
            // JPA will validate foreign keys on flush/commit
            Patient patientRef = entityManager.getReference(Patient.class, patientId);
            Doctor doctorRef = entityManager.getReference(Doctor.class, doctorId);

            // Create appointment session
            AppointmentSession session = AppointmentSession.builder()
                .patient(patientRef)
                .doctor(doctorRef)
                .scheduledDateTime(scheduledDateTime)
                .consultations(consultations)
                .isEmergency(isEmergency)
                .status(SessionStatus.SCHEDULED)
                .build();

            AppointmentSession savedSession = appointmentRepository.save(session);

            // Publish domain event for cross-service communication
            eventPublisher.publishEvent(new AppointmentScheduled(
                savedSession.getSessionId(), patientId, doctorId,
                scheduledDateTime, consultationNames, isEmergency));

            log.info("Appointment scheduled: {} for patient {} with doctor {} at {}",
                savedSession.getSessionId(), patientId, doctorId, scheduledDateTime);

            return Result.success(appointmentMapper.toDto(savedSession));

        } catch (Exception e) {
            log.error("Error scheduling appointment", e);
            return Result.failure("Failed to schedule appointment: " + e.getMessage());
        }
    }

    /**
     * Adds a consultation to an existing session.
     *
     * Architecture notes:
     * - Uses EntityGraph to load session with consultations (prevents N+1 queries)
     * - Gets consultation entity via ConsultationService
     *
     * @param sessionId Session identifier
     * @param consultationName Name of consultation to add
     * @return Result containing updated AppointmentSessionDto or error message
     */
    public Result<AppointmentSessionDto> addConsultationToSession(UUID sessionId, String consultationName) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }
            if (consultationName == null || consultationName.trim().isEmpty()) {
                return Result.failure("Consultation name is required");
            }

            // Use EntityGraph to load session with consultations collection
            AppointmentSession session = appointmentRepository.findWithConsultationsBySessionId(sessionId)
                .orElse(null);
            if (session == null) {
                return Result.failure("Session not found");
            }

            if (session.getStatus() != SessionStatus.SCHEDULED &&
                session.getStatus() != SessionStatus.IN_PROGRESS) {
                return Result.failure("Cannot add consultations to completed sessions");
            }

            // Get consultation entity via ConsultationService
            Consultation consultation = consultationService.getEntityByName(consultationName.trim());
            if (consultation == null) {
                return Result.failure("Consultation not found or inactive");
            }

            // Add consultation to session
            session.getConsultations().add(consultation);
            AppointmentSession savedSession = appointmentRepository.save(session);

            // Publish domain event
            eventPublisher.publishEvent(new ConsultationTypeAdded(
                sessionId, consultationName, true, consultation.getPrice()));

            log.info("Consultation {} added to session {}", consultationName, sessionId);

            return Result.success(appointmentMapper.toDto(savedSession));

        } catch (Exception e) {
            log.error("Error adding consultation to session", e);
            return Result.failure("Failed to add consultation: " + e.getMessage());
        }
    }

    /**
     * Starts an appointment session.
     *
     * Architecture notes:
     * - Uses EntityGraph to load patient and doctor for event publishing
     *
     * @param sessionId Session identifier
     * @return Result containing updated AppointmentSessionDto or error message
     */
    public Result<AppointmentSessionDto> startSession(UUID sessionId) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }

            // Use EntityGraph to load session with basic relationships
            AppointmentSession session = appointmentRepository.findWithBasicRelationshipsBySessionId(sessionId)
                .orElse(null);
            if (session == null) {
                return Result.failure("Session not found");
            }

            if (session.getStatus() != SessionStatus.SCHEDULED) {
                return Result.failure("Only scheduled sessions can be started");
            }

            session.setStatus(SessionStatus.IN_PROGRESS);
            AppointmentSession savedSession = appointmentRepository.save(session);

            // Publish domain event (patient and doctor already loaded via EntityGraph)
            eventPublisher.publishEvent(new SessionStarted(
                sessionId, session.getPatient().getPatientId(),
                session.getDoctor().getDoctorId(), OffsetDateTime.now(ZoneOffset.UTC)));

            log.info("Session started: {}", sessionId);

            return Result.success(appointmentMapper.toDto(savedSession));

        } catch (Exception e) {
            log.error("Error starting session", e);
            return Result.failure("Failed to start session: " + e.getMessage());
        }
    }

    /**
     * Adds medical information to an in-progress session.
     *
     * Architecture notes:
     * - Gets diagnosis entities via DiagnosisService (internal method)
     * - No EntityGraph needed here - minimal relationship access
     *
     * @param sessionId Session identifier
     * @param diagnosisIds List of diagnosis identifiers
     * @param freeTextDiagnosis Free-text diagnosis
     * @param treatmentInstructions Treatment instructions
     * @param freeTextObservations Additional observations
     * @return Result containing updated AppointmentSessionDto or error message
     */
    public Result<AppointmentSessionDto> addMedicalInformation(UUID sessionId,
                                                             List<UUID> diagnosisIds,
                                                             String freeTextDiagnosis,
                                                             String treatmentInstructions,
                                                             String freeTextObservations) {
        try {
            AppointmentSession session = appointmentRepository.findById(sessionId)
                .orElse(null);
            if (session == null) {
                return Result.failure("Session not found");
            }

            if (session.getStatus() != SessionStatus.IN_PROGRESS) {
                return Result.failure("Can only add medical information to in-progress sessions");
            }

            // Update medical text fields
            session.setFreeTextDiagnosis(freeTextDiagnosis);
            session.setTreatmentInstructions(treatmentInstructions);
            session.setFreeTextObservations(freeTextObservations);

            // Get diagnosis entities via DiagnosisService if provided
            if (diagnosisIds != null && !diagnosisIds.isEmpty()) {
                List<Diagnosis> diagnoses = diagnosisService.getEntitiesByIds(diagnosisIds);
                session.setDiagnoses(diagnoses);
            }

            AppointmentSession savedSession = appointmentRepository.save(session);

            log.info("Medical information added to session: {}", sessionId);

            return Result.success(appointmentMapper.toDto(savedSession));

        } catch (Exception e) {
            log.error("Error adding medical information", e);
            return Result.failure("Failed to add medical information: " + e.getMessage());
        }
    }

    /**
     * Completes an appointment session with medical documentation.
     *
     * Architecture notes:
     * - Uses EntityGraph to load all relationships for event publishing and DTO mapping
     * - Publishes two events: SessionDocumentationCompleted and SessionCompleted
     *
     * @param sessionId Session identifier
     * @param freeTextDiagnosis Free-text diagnosis
     * @param treatmentInstructions Treatment instructions
     * @param freeTextObservations Additional observations
     * @return Result containing updated AppointmentSessionDto or error message
     */
    public Result<AppointmentSessionDto> completeSession(UUID sessionId, String freeTextDiagnosis,
                                                        String treatmentInstructions, String freeTextObservations) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }

            // Use EntityGraph to load all relationships for event publishing
            AppointmentSession session = appointmentRepository.findWithAllRelationshipsBySessionId(sessionId)
                .orElse(null);
            if (session == null) {
                return Result.failure("Session not found");
            }

            if (session.getStatus() != SessionStatus.IN_PROGRESS) {
                return Result.failure("Only in-progress sessions can be completed");
            }

            // Update medical information and status
            session.setFreeTextDiagnosis(freeTextDiagnosis);
            session.setTreatmentInstructions(treatmentInstructions);
            session.setFreeTextObservations(freeTextObservations);
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));

            AppointmentSession savedSession = appointmentRepository.save(session);

            // Publish domain events (relationships already loaded via EntityGraph)
            eventPublisher.publishEvent(new SessionDocumentationCompleted(
                sessionId, session.getPatient().getPatientId(),
                session.getDoctor().getDoctorId(), freeTextDiagnosis,
                treatmentInstructions, getConsultationNames(session)));

            eventPublisher.publishEvent(new SessionCompleted(
                sessionId, session.getPatient().getPatientId(),
                session.getDoctor().getDoctorId(), OffsetDateTime.now(ZoneOffset.UTC),
                getConsultationNames(session)));

            log.info("Session completed: {}", sessionId);

            return Result.success(appointmentMapper.toDto(savedSession));

        } catch (Exception e) {
            log.error("Error completing session", e);
            return Result.failure("Failed to complete session: " + e.getMessage());
        }
    }

    /**
     * Cancels an appointment.
     *
     * Architecture notes:
     * - Uses EntityGraph to load patient for event publishing
     *
     * @param sessionId Session identifier
     * @param reason Cancellation reason
     * @param wasNoShow Whether this was a no-show
     * @return Result containing updated AppointmentSessionDto or error message
     */
    public Result<AppointmentSessionDto> cancelAppointment(UUID sessionId, String reason, boolean wasNoShow) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }

            // Use EntityGraph to load session with basic relationships
            AppointmentSession session = appointmentRepository.findWithBasicRelationshipsBySessionId(sessionId)
                .orElse(null);
            if (session == null) {
                return Result.failure("Session not found");
            }

            SessionStatus newStatus = wasNoShow ? SessionStatus.NO_SHOW : SessionStatus.CANCELLED;
            session.setStatus(newStatus);
            session.setCancellationReason(reason);
            session.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));

            AppointmentSession savedSession = appointmentRepository.save(session);

            // Publish domain event (patient already loaded via EntityGraph)
            eventPublisher.publishEvent(new AppointmentCancelled(
                sessionId, session.getPatient().getPatientId(), reason, wasNoShow));

            log.info("Appointment cancelled: {} (wasNoShow: {})", sessionId, wasNoShow);

            return Result.success(appointmentMapper.toDto(savedSession));

        } catch (Exception e) {
            log.error("Error cancelling appointment", e);
            return Result.failure("Failed to cancel appointment: " + e.getMessage());
        }
    }

    /**
     * Retrieves patient's appointment history.
     *
     * Architecture notes:
     * - Uses EntityGraph to load all relationships (prevents N+1 queries - HUGE performance benefit!)
     * - All data loaded in single query for DTO mapping
     *
     * @param patientId Patient identifier
     * @return Result containing list of AppointmentSessionDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<AppointmentSessionDto>> getPatientAppointmentHistory(UUID patientId) {
        try {
            // Use EntityGraph to load all relationships in single query
            // Without this, mapping to DTO would cause N+1 queries for each relationship
            List<AppointmentSession> sessions = appointmentRepository
                .findWithRelationshipsByPatientPatientIdOrderByScheduledDateTimeDesc(patientId);

            List<AppointmentSessionDto> sessionDtos = sessions.stream()
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());

            return Result.success(sessionDtos);

        } catch (Exception e) {
            log.error("Error getting patient appointment history", e);
            return Result.failure("Failed to get appointment history: " + e.getMessage());
        }
    }

    /**
     * PUBLIC: Retrieves a single appointment session by ID with all relationships loaded.
     * Uses EntityGraph to prevent N+1 queries.
     *
     * @param sessionId Session identifier
     * @return Result containing AppointmentSessionDto or error message
     */
    @Transactional(readOnly = true)
    public Result<AppointmentSessionDto> findById(UUID sessionId) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }

            AppointmentSession session = appointmentRepository
                .findWithAllRelationshipsBySessionId(sessionId)
                .orElse(null);

            if (session == null) {
                return Result.failure("Appointment session not found with ID: " + sessionId);
            }

            return Result.success(appointmentMapper.toDto(session));

        } catch (Exception e) {
            log.error("Error getting appointment session by ID", e);
            return Result.failure("Failed to get appointment session: " + e.getMessage());
        }
    }

    private List<String> getConsultationNames(AppointmentSession session) {
        return session.getConsultations().stream()
            .map(Consultation::getName)
            .collect(Collectors.toList());
    }

    /**
     * INTERNAL: Validates that a question belongs to one of the session's consultations.
     * Used by AnswerService to ensure data integrity when saving answers.
     * Uses EntityGraph to load consultations efficiently.
     *
     * @param sessionId Session identifier
     * @param questionConsultationId The consultation ID from the question
     * @return Result success if question's consultation is in session, failure with message otherwise
     */
    @Transactional(readOnly = true)
    public Result<Void> validateQuestionBelongsToSession(UUID sessionId, UUID questionConsultationId) {
        if (sessionId == null) {
            return Result.failure("Session ID is required");
        }
        if (questionConsultationId == null) {
            return Result.failure("Question consultation ID is required");
        }

        // Use EntityGraph to load session with consultations
        AppointmentSession session = appointmentRepository.findWithConsultationsBySessionId(sessionId)
            .orElse(null);
        if (session == null) {
            return Result.failure("Session not found");
        }

        // Validate that question's consultation is in session's consultations
        boolean validQuestion = session.getConsultations().stream()
            .anyMatch(c -> c.getConsultationId().equals(questionConsultationId));

        if (!validQuestion) {
            return Result.failure("Question does not belong to any consultation in this session");
        }

        return Result.success(null);
    }

    /**
     * INTERNAL: Gets appointment session entity with all relationships loaded.
     * Used by other services (e.g., BillingService) when they need session data.
     * Uses EntityGraph to load all relationships efficiently.
     *
     * @param sessionId Session identifier
     * @return AppointmentSession entity with all relationships or null if not found
     */
    @Transactional(readOnly = true)
    public AppointmentSession getEntityWithAllRelationships(UUID sessionId) {
        if (sessionId == null) {
            return null;
        }
        return appointmentRepository.findWithAllRelationshipsBySessionId(sessionId).orElse(null);
    }

    /**
     * INTERNAL: Validates that a session has completed status.
     * Used by other services (e.g., BillingService) to validate session is completed.
     * Returns Result for clear error messaging.
     *
     * @param sessionId Session identifier
     * @return Result success if session is completed, failure with message otherwise
     */
    @Transactional(readOnly = true)
    public Result<Void> validateSessionCompleted(UUID sessionId) {
        if (sessionId == null) {
            return Result.failure("Session ID is required");
        }

        AppointmentSession session = appointmentRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return Result.failure("Session not found");
        }

        if (session.getStatus() != SessionStatus.COMPLETED) {
            return Result.failure("Can only create billing for completed sessions");
        }

        return Result.success(null);
    }

    /**
     * INTERNAL: Checks if doctor has appointments with patient in date range.
     * Used by MedicalFileAccessService for access control.
     *
     * @param doctorId Doctor identifier
     * @param patientId Patient identifier
     * @param fromDate Start date
     * @param toDate End date
     * @param excludeStatus Status to exclude (e.g., CANCELLED)
     * @return true if appointments exist, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasAppointmentsInRange(UUID doctorId, UUID patientId,
                                         OffsetDateTime fromDate, OffsetDateTime toDate,
                                         SessionStatus excludeStatus) {
        if (doctorId == null || patientId == null || fromDate == null || toDate == null) {
            return false;
        }

        return appointmentRepository
            .existsByDoctorDoctorIdAndPatientPatientIdAndScheduledDateTimeBetweenAndStatusNot(
                doctorId, patientId, fromDate, toDate, excludeStatus);
    }

    /**
     * INTERNAL: Gets all future appointments for a doctor in date range.
     * Used by MedicalFileAccessService to get accessible patients.
     * Uses EntityGraph to load patients efficiently.
     *
     * @param doctorId Doctor identifier
     * @param fromDate Start date
     * @param toDate End date
     * @param excludeStatus Status to exclude (e.g., CANCELLED)
     * @return List of AppointmentSession entities with patient loaded
     */
    @Transactional(readOnly = true)
    public List<AppointmentSession> getAppointmentsInRangeWithPatient(UUID doctorId,
                                                                      OffsetDateTime fromDate,
                                                                      OffsetDateTime toDate,
                                                                      SessionStatus excludeStatus) {
        if (doctorId == null || fromDate == null || toDate == null) {
            return List.of();
        }

        // Use EntityGraph to load appointments with patients
        return appointmentRepository
            .findWithBasicRelationshipsByDoctorDoctorIdAndScheduledDateTimeBetweenAndStatusNot(
                doctorId, fromDate, toDate, excludeStatus);
    }
}
