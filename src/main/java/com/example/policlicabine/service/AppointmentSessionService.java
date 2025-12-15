package com.example.policlicabine.service;

import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.dto.AppointmentSessionFilterCriteria;
import com.example.policlicabine.dto.FormReadinessDto;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.SessionStatus;
import com.example.policlicabine.event.SessionCompleted;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.AppointmentSessionMapper;
import com.example.policlicabine.repository.AppointmentSessionRepository;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.repository.FormSubmissionRepository;
import com.example.policlicabine.specification.AppointmentSessionSpecificationBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentSessionService {

    private final AppointmentSessionRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final ConsultationService consultationService;
    private final DiagnosisService diagnosisService;
    private final FormSubmissionService formSubmissionService;
    private final FormReadinessService formReadinessService;
    private final AppointmentSessionMapper appointmentMapper;
    private final AppointmentSessionSpecificationBuilder specificationBuilder;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    public AppointmentSessionDto scheduleAppointment(UUID patientId, UUID doctorId,
                                                     List<String> consultationNames,
                                                     OffsetDateTime scheduledDateTime,
                                                     boolean isEmergency) {
        if (consultationNames == null || consultationNames.isEmpty()) {
            throw new BusinessException("At least one consultation is required");
        }
        if (scheduledDateTime == null) {
            throw new BusinessException("Scheduled date and time is required");
        }

        patientService.validatePatientExists(patientId);
        doctorService.validateDoctorExists(doctorId);

        List<ConsultationType> consultations = consultationService.getEntitiesByNames(consultationNames);
        if (consultations.size() != consultationNames.size()) {
            throw new BusinessException("Some consultations not found or inactive");
        }

        Patient patientRef = entityManager.getReference(Patient.class, patientId);
        Doctor doctorRef = entityManager.getReference(Doctor.class, doctorId);

        AppointmentSession session = AppointmentSession.builder()
            .patient(patientRef)
            .doctor(doctorRef)
            .scheduledDateTime(scheduledDateTime)
            .consultationTypes(consultations)
            .isEmergency(isEmergency)
            .status(SessionStatus.SCHEDULED)
            .build();

        AppointmentSession savedSession = appointmentRepository.save(session);

        log.info("Appointment scheduled: {} for patient {} with doctor {} at {}",
            savedSession.getSessionId(), patientId, doctorId, scheduledDateTime);

        return appointmentMapper.toDto(savedSession);
    }

    public AppointmentSessionDto addConsultationToSession(UUID sessionId, String consultationName) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }
        if (consultationName == null || consultationName.trim().isEmpty()) {
            throw new BusinessException("ConsultationType name is required");
        }

        AppointmentSession session = appointmentRepository.findWithConsultationsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.SCHEDULED &&
            session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException("Cannot add consultations to completed sessions");
        }

        ConsultationType consultation = consultationService.getEntityByName(consultationName.trim());
        if (consultation == null) {
            throw new ResourceNotFoundException("ConsultationType", "name: " + consultationName);
        }

        session.getConsultationTypes().add(consultation);
        AppointmentSession savedSession = appointmentRepository.save(session);

        log.info("ConsultationType {} added to session {}", consultationName, sessionId);

        return appointmentMapper.toDto(savedSession);
    }

    public AppointmentSessionDto removeConsultationFromSession(UUID sessionId, UUID consultationId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }
        if (consultationId == null) {
            throw new BusinessException("Consultation ID is required");
        }

        AppointmentSession session = appointmentRepository.findWithConsultationsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.SCHEDULED &&
            session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException("Cannot remove consultations from completed sessions");
        }

        ConsultationType consultationToRemove = session.getConsultationTypes().stream()
            .filter(c -> c.getConsultationId().equals(consultationId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Consultation not found in session"));

        session.getConsultationTypes().remove(consultationToRemove);
        AppointmentSession savedSession = appointmentRepository.save(session);

        log.info("ConsultationType {} removed from session {}", consultationId, sessionId);

        return appointmentMapper.toDto(savedSession);
    }

    public AppointmentSessionDto startSession(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        AppointmentSession session = appointmentRepository.findWithBasicRelationshipsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException("Only scheduled sessions can be started");
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        AppointmentSession savedSession = appointmentRepository.save(session);

        log.info("Session started: {}", sessionId);

        return appointmentMapper.toDto(savedSession);
    }

    public AppointmentSessionDto addMedicalInformation(UUID sessionId,
                                                       List<UUID> diagnosisIds,
                                                       String freeTextDiagnosis,
                                                       String treatmentInstructions,
                                                       String freeTextObservations) {
        AppointmentSession session = appointmentRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException("Can only add medical information to in-progress sessions");
        }

        session.setFreeTextDiagnosis(freeTextDiagnosis);
        session.setTreatmentInstructions(treatmentInstructions);
        session.setFreeTextObservations(freeTextObservations);

        if (diagnosisIds != null && !diagnosisIds.isEmpty()) {
            List<Diagnosis> diagnoses = diagnosisService.getEntitiesByIds(diagnosisIds);
            session.setDiagnoses(diagnoses);
        }

        AppointmentSession savedSession = appointmentRepository.save(session);

        log.info("Medical information added to session: {}", sessionId);

        return appointmentMapper.toDto(savedSession);
    }

    public AppointmentSessionDto completeSession(UUID sessionId, String freeTextDiagnosis,
                                                 String treatmentInstructions, String freeTextObservations) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        AppointmentSession session = appointmentRepository.findWithAllRelationshipsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException("Only in-progress sessions can be completed");
        }

        session.setFreeTextDiagnosis(freeTextDiagnosis);
        session.setTreatmentInstructions(treatmentInstructions);
        session.setFreeTextObservations(freeTextObservations);
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        AppointmentSession savedSession = appointmentRepository.save(session);

        eventPublisher.publishEvent(new SessionCompleted(
            sessionId, session.getPatient().getPatientId(),
            session.getDoctor().getDoctorId(), OffsetDateTime.now(ZoneOffset.UTC),
            session.getConsultationNames()));

        log.info("Session completed: {}", sessionId);

        return appointmentMapper.toDto(savedSession);
    }

    public AppointmentSessionDto cancelAppointment(UUID sessionId, String reason, boolean wasNoShow) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        AppointmentSession session = appointmentRepository.findWithBasicRelationshipsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        SessionStatus newStatus = wasNoShow ? SessionStatus.NO_SHOW : SessionStatus.CANCELLED;
        session.setStatus(newStatus);
        session.setCancellationReason(reason);
        session.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));

        AppointmentSession savedSession = appointmentRepository.save(session);

        log.info("Appointment cancelled: {} (wasNoShow: {})", sessionId, wasNoShow);

        return appointmentMapper.toDto(savedSession);
    }

    @Transactional(readOnly = true)
    public List<AppointmentSessionDto> getPatientAppointmentHistory(UUID patientId) {
        List<AppointmentSession> sessions = appointmentRepository
            .findWithRelationshipsByPatientPatientIdOrderByScheduledDateTimeDesc(patientId);

        List<AppointmentSessionDto> dtos = sessions.stream()
            .map(appointmentMapper::toDto)
            .collect(Collectors.toList());

        // Enrich DTOs with form counts (required, completed, allFormsComplete)
        enrichWithFormStatusBatch(dtos, sessions);

        return dtos;
    }

    @Transactional(readOnly = true)
    public AppointmentSessionDto findById(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        AppointmentSession session = appointmentRepository
            .findWithAllRelationshipsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("AppointmentSession", sessionId));

        return appointmentMapper.toDto(session);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentSessionDto> search(AppointmentSessionFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching appointment sessions with criteria: {} and pageable: {}", criteria, pageable);

        Specification<AppointmentSession> spec = specificationBuilder.build(criteria);
        Page<AppointmentSession> entityPage = appointmentRepository.findAll(spec, pageable);

        List<AppointmentSessionDto> dtos = entityPage.getContent().stream()
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());

        enrichWithFormStatusBatch(dtos, entityPage.getContent());

        Page<AppointmentSessionDto> dtoPage = new PageImpl<>(dtos, pageable, entityPage.getTotalElements());

        log.info("Appointment session search returned {} results (page {}/{})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber() + 1,
                dtoPage.getTotalPages());

        return dtoPage;
    }

    @Transactional(readOnly = true)
    public void validateQuestionBelongsToSession(UUID sessionId, UUID questionConsultationId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }
        if (questionConsultationId == null) {
            throw new BusinessException("Question consultation ID is required");
        }

        AppointmentSession session = appointmentRepository.findWithConsultationsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        boolean validQuestion = session.getConsultationTypes().stream()
            .anyMatch(c -> c.getConsultationId().equals(questionConsultationId));

        if (!validQuestion) {
            throw new BusinessException("Question does not belong to any consultation in this session");
        }
    }

    @Transactional(readOnly = true)
    public AppointmentSession getEntityWithAllRelationships(UUID sessionId) {
        if (sessionId == null) {
            return null;
        }
        return appointmentRepository.findWithAllRelationshipsBySessionId(sessionId).orElse(null);
    }

    @Transactional(readOnly = true)
    public void validateSessionCompleted(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        AppointmentSession session = appointmentRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new BusinessException("Can only create billing for completed sessions");
        }
    }

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

    @Transactional(readOnly = true)
    public List<AppointmentSession> getAppointmentsInRangeWithPatient(UUID doctorId,
                                                                      OffsetDateTime fromDate,
                                                                      OffsetDateTime toDate,
                                                                      SessionStatus excludeStatus) {
        if (doctorId == null || fromDate == null || toDate == null) {
            return List.of();
        }

        return appointmentRepository
            .findWithBasicRelationshipsByDoctorDoctorIdAndScheduledDateTimeBetweenAndStatusNot(
                doctorId, fromDate, toDate, excludeStatus);
    }

    @Transactional(readOnly = true)
    public List<FormTemplateDto> getRequiredFormsForSession(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        AppointmentSession session = appointmentRepository.findWithConsultationsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        List<UUID> consultationIds = session.getConsultationTypes().stream()
                .map(ConsultationType::getConsultationId)
                .toList();

        List<ConsultationType> consultationsWithTemplates = consultationService.getEntitiesWithFormTemplatesByIds(consultationIds);

        Set<FormTemplate> allRequiredTemplates = consultationsWithTemplates.stream()
                .filter(ct -> ct.getRequiredFormTemplates() != null)
                .flatMap(ct -> ct.getRequiredFormTemplates().stream())
                .collect(Collectors.toSet());

        return allRequiredTemplates.stream()
                .filter(t -> t.getActive() && !t.getIsDeleted())
                .map(this::toFormTemplateDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormTemplateDto> getMissingFormsForSession(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        AppointmentSession session = appointmentRepository.findWithBasicRelationshipsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        List<FormTemplateDto> requiredForms = getRequiredFormsForSession(sessionId);

        UUID patientId = session.getPatient().getPatientId();
        LocalDateTime appointmentDateTime = session.getScheduledDateTime().toLocalDateTime();

        return requiredForms.stream()
                .filter(template -> !formSubmissionService.hasValidSubmission(patientId, template.getId(), appointmentDateTime))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormReadinessDto getFormReadiness(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        AppointmentSession session = appointmentRepository.findWithConsultationsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        List<UUID> consultationTypeIds = session.getConsultationTypes().stream()
                .map(ConsultationType::getConsultationId)
                .collect(Collectors.toList());

        LocalDateTime appointmentDate = session.getScheduledDateTime().toLocalDateTime();
        UUID patientId = session.getPatient().getPatientId();

        FormReadinessDto readiness = formReadinessService.checkReadiness(patientId, consultationTypeIds, appointmentDate);
        readiness.setAppointmentSessionId(sessionId);

        return readiness;
    }

    @Transactional(readOnly = true)
    public FormReadinessDto checkFormReadiness(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        log.debug("Checking form readiness for session: {}", sessionId);

        AppointmentSession session = appointmentRepository.findWithConsultationsBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("AppointmentSession", sessionId));

        if (session.getConsultationTypes() == null || session.getConsultationTypes().isEmpty()) {
            log.warn("Session {} has no consultation types configured", sessionId);
            throw new BusinessException("No consultations configured for this appointment");
        }

        List<UUID> consultationTypeIds = session.getConsultationTypes().stream()
                .map(ConsultationType::getConsultationId)
                .toList();

        FormReadinessDto dto = formReadinessService.checkReadiness(
                session.getPatient().getPatientId(),
                consultationTypeIds,
                session.getScheduledDateTime().toLocalDateTime()
        );

        dto.setAppointmentSessionId(sessionId);
        log.info("Form readiness check for session {}: {} of {} forms valid, all complete: {}",
                sessionId, dto.getValidCount(), dto.getTotalRequired(), dto.isAllFormsComplete());

        return dto;
    }

    private FormTemplateDto toFormTemplateDto(FormTemplate template) {
        return FormTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .active(template.getActive())
                .structure(template.getStructure())
                .validityMonths(template.getValidityMonths())
                .pdfTemplateUrl(template.getPdfTemplateUrl())
                .createdAt(template.getCreatedAt())
                .createdByUserId(template.getCreatedBy() != null ? template.getCreatedBy().getUserId() : null)
                .build();
    }

    private void enrichWithFormStatusBatch(List<AppointmentSessionDto> dtos, List<AppointmentSession> sessions) {
        if (dtos.isEmpty()) {
            return;
        }

        Set<UUID> consultationIds = sessions.stream()
                .filter(s -> s.getConsultationTypes() != null)
                .flatMap(s -> s.getConsultationTypes().stream())
                .map(ConsultationType::getConsultationId)
                .collect(Collectors.toSet());

        if (consultationIds.isEmpty()) {
            dtos.forEach(dto -> {
                dto.setRequiredFormsCount(0);
                dto.setCompletedFormsCount(0);
                dto.setAllFormsComplete(true);
            });
            return;
        }

        List<ConsultationType> consultationsWithTemplates = consultationRepository
                .findWithRequiredFormTemplatesByConsultationIdIn(new ArrayList<>(consultationIds));

        Map<UUID, Set<FormTemplate>> templatesByConsultation = consultationsWithTemplates.stream()
                .collect(Collectors.toMap(
                        ConsultationType::getConsultationId,
                        c -> c.getRequiredFormTemplates() != null ? c.getRequiredFormTemplates() : Set.of()
                ));

        Set<UUID> patientIds = sessions.stream()
                .filter(s -> s.getPatient() != null)
                .map(s -> s.getPatient().getPatientId())
                .collect(Collectors.toSet());

        List<FormSubmission> allSubmissions = patientIds.isEmpty() ? List.of() :
                formSubmissionRepository.findByPatientPatientIdInAndIsDeletedFalse(new ArrayList<>(patientIds));

        Map<UUID, Map<UUID, FormSubmission>> submissionsByPatient = groupSubmissionsByPatientAndTemplate(allSubmissions);

        for (int i = 0; i < dtos.size(); i++) {
            AppointmentSessionDto dto = dtos.get(i);
            AppointmentSession session = sessions.get(i);

            if (session.getConsultationTypes() == null || session.getConsultationTypes().isEmpty()) {
                dto.setRequiredFormsCount(0);
                dto.setCompletedFormsCount(0);
                dto.setAllFormsComplete(true);
                continue;
            }

            Set<FormTemplate> requiredTemplates = session.getConsultationTypes().stream()
                    .flatMap(c -> templatesByConsultation.getOrDefault(c.getConsultationId(), Set.of()).stream())
                    .filter(t -> t.getActive() && !t.getIsDeleted())
                    .collect(Collectors.toSet());

            int requiredCount = requiredTemplates.size();

            if (requiredCount == 0) {
                dto.setRequiredFormsCount(0);
                dto.setCompletedFormsCount(0);
                dto.setAllFormsComplete(true);
                continue;
            }

            UUID patientId = session.getPatient().getPatientId();
            LocalDateTime appointmentDate = session.getScheduledDateTime().toLocalDateTime();
            Map<UUID, FormSubmission> patientSubmissions = submissionsByPatient.getOrDefault(patientId, Map.of());

            int completedCount = (int) requiredTemplates.stream()
                    .filter(template -> {
                        FormSubmission submission = patientSubmissions.get(template.getId());
                        return submission != null && isValidAtDate(submission, appointmentDate);
                    })
                    .count();

            dto.setRequiredFormsCount(requiredCount);
            dto.setCompletedFormsCount(completedCount);
            dto.setAllFormsComplete(requiredCount == completedCount);
        }
    }

    private boolean isValidAtDate(FormSubmission submission, LocalDateTime targetDate) {
        return submission.getExpiresAt() == null || submission.getExpiresAt().isAfter(targetDate);
    }

    private Map<UUID, Map<UUID, FormSubmission>> groupSubmissionsByPatientAndTemplate(List<FormSubmission> submissions) {
        return submissions.stream()
                .filter(s -> s.getPatient() != null && s.getTemplate() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getPatient().getPatientId(),
                        Collectors.toMap(
                                s -> s.getTemplate().getId(),
                                s -> s,
                                (s1, s2) -> s1.getSubmittedAt().isAfter(s2.getSubmittedAt()) ? s1 : s2
                        )
                ));
    }
}
