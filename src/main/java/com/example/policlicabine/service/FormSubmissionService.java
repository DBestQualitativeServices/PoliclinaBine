package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.FormSubmissionDto;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.entity.enums.SubmissionStatus;
import com.example.policlicabine.mapper.FormSubmissionMapper;
import com.example.policlicabine.repository.FormSubmissionRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class FormSubmissionService extends BaseServiceImpl<FormSubmission, FormSubmissionDto, UUID> {

    private final FormSubmissionRepository formSubmissionRepository;
    private final FormSubmissionMapper formSubmissionMapper;
    private final FormTemplateService formTemplateService;
    private final FormValidationService formValidationService;
    private final PatientService patientService;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    public FormSubmissionService(FormSubmissionRepository repository, FormSubmissionMapper mapper,
                                  FormTemplateService formTemplateService, FormValidationService formValidationService,
                                  PatientService patientService, ApplicationEventPublisher eventPublisher) {
        super(repository, mapper);
        this.formSubmissionRepository = repository;
        this.formSubmissionMapper = mapper;
        this.formTemplateService = formTemplateService;
        this.formValidationService = formValidationService;
        this.patientService = patientService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected FormSubmissionDto toDto(FormSubmission entity) {
        return formSubmissionMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "FormSubmission";
    }

    @Override
    protected void updateEntityFromDto(FormSubmission entity, FormSubmissionDto dto) {
        if (dto.getData() != null) {
            entity.setData(dto.getData());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<FormSubmissionDto> findById(UUID id) {
        FormSubmission submission = formSubmissionRepository.findWithDetailsById(id).orElse(null);
        if (submission == null || submission.getIsDeleted()) {
            return Result.failure("FormSubmission not found");
        }
        return Result.success(formSubmissionMapper.toDto(submission));
    }

    @Transactional
    public Result<FormSubmissionDto> submitForm(UUID templateId, UUID patientId, Map<String, Object> data,
                                                 UUID appointmentSessionId, UUID consultationTypeId, UUID submittedByUserId) {
        if (templateId == null) {
            return Result.failure("Template ID is required");
        }
        if (patientId == null) {
            return Result.failure("Patient ID is required");
        }
        if (data == null || data.isEmpty()) {
            return Result.failure("Form data is required");
        }

        FormTemplate template = formTemplateService.getEntityById(templateId);
        if (template == null) {
            return Result.failure("Form template not found");
        }

        if (!template.getActive()) {
            return Result.failure("Form template is not active");
        }

        Result<Void> patientCheck = patientService.validateExists(patientId);
        if (patientCheck.isFailure()) {
            return Result.failure(patientCheck.getErrorMessage());
        }

        List<String> validationErrors = formValidationService.validate(template.getStructure(), data);
        if (!validationErrors.isEmpty()) {
            return Result.failure("Validation failed: " + String.join(", ", validationErrors));
        }

        Patient patientRef = entityManager.getReference(Patient.class, patientId);
        AppointmentSession sessionRef = appointmentSessionId != null ?
                entityManager.getReference(AppointmentSession.class, appointmentSessionId) : null;
        ConsultationType consultationTypeRef = consultationTypeId != null ?
                entityManager.getReference(ConsultationType.class, consultationTypeId) : null;
        User submittedByRef = submittedByUserId != null ?
                entityManager.getReference(User.class, submittedByUserId) : null;

        LocalDateTime expiresAt = null;
        if (template.getValidityMonths() != null && template.getValidityMonths() > 0) {
            expiresAt = LocalDateTime.now().plusMonths(template.getValidityMonths());
        }

        FormSubmission submission = FormSubmission.builder()
                .template(template)
                .patient(patientRef)
                .appointmentSession(sessionRef)
                .consultationType(consultationTypeRef)
                .templateSnapshot(template.getStructure())
                .data(data)
                .status(SubmissionStatus.PENDING_SIGNATURE)
                .submittedBy(submittedByRef)
                .expiresAt(expiresAt)
                .build();

        FormSubmission saved = formSubmissionRepository.save(submission);
        log.info("Form submitted: {} for patient {}", template.getName(), patientId);

        return Result.success(formSubmissionMapper.toDto(saved));
    }

    @Transactional
    public Result<FormSubmissionDto> signForm(UUID submissionId, UUID witnessedByUserId) {
        if (submissionId == null) {
            return Result.failure("Submission ID is required");
        }
        if (witnessedByUserId == null) {
            return Result.failure("Witness user ID is required");
        }

        FormSubmission submission = formSubmissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            return Result.failure("Form submission not found");
        }

        if (submission.getStatus() == SubmissionStatus.SIGNED) {
            return Result.failure("Form is already signed");
        }

        if (submission.isExpired()) {
            return Result.failure("Form has expired");
        }

        User witnessedBy = entityManager.getReference(User.class, witnessedByUserId);
        submission.signByPatient(witnessedBy);

        FormSubmission saved = formSubmissionRepository.save(submission);
        log.info("Form signed: {} by witness {}", submissionId, witnessedByUserId);

        return Result.success(formSubmissionMapper.toDto(saved));
    }

    @Transactional(readOnly = true)
    public Result<Boolean> hasValidForm(UUID patientId, FormPurpose purpose) {
        if (patientId == null || purpose == null) {
            return Result.failure("Patient ID and purpose are required");
        }

        FormSubmission submission = formSubmissionRepository
                .findValidFormByPatientAndPurpose(patientId, purpose.name(), LocalDateTime.now())
                .orElse(null);

        return Result.success(submission != null && submission.isValid());
    }

    /**
     * Checks if patient has a valid form for a purpose that will still be valid at a target date.
     * Used for appointment scheduling to verify forms won't expire before the appointment.
     *
     * @param patientId Patient identifier
     * @param purpose Form purpose to check
     * @param targetDate Date when form must still be valid (typically appointment date)
     * @return Result containing true if valid form exists at target date, false otherwise
     */
    @Transactional(readOnly = true)
    public Result<Boolean> hasValidFormAtDate(UUID patientId, FormPurpose purpose, LocalDateTime targetDate) {
        if (patientId == null || purpose == null || targetDate == null) {
            return Result.failure("Patient ID, purpose, and target date are required");
        }

        // Check for form that never expires (expiresAt IS NULL)
        Optional<FormSubmission> neverExpires = formSubmissionRepository
                .findFirstByPatientPatientIdAndTemplatePurposeAndStatusAndIsDeletedFalseAndExpiresAtIsNullOrderBySubmittedAtDesc(
                        patientId, purpose, SubmissionStatus.SIGNED);

        if (neverExpires.isPresent()) {
            return Result.success(true);
        }

        // Check for form that expires after target date
        Optional<FormSubmission> expiresAfter = formSubmissionRepository
                .findFirstByPatientPatientIdAndTemplatePurposeAndStatusAndIsDeletedFalseAndExpiresAtGreaterThanOrderBySubmittedAtDesc(
                        patientId, purpose, SubmissionStatus.SIGNED, targetDate);

        return Result.success(expiresAfter.isPresent());
    }

    @Transactional(readOnly = true)
    public Result<List<FormSubmissionDto>> getFormsByPatient(UUID patientId) {
        if (patientId == null) {
            return Result.failure("Patient ID is required");
        }

        List<FormSubmission> submissions = formSubmissionRepository.findByPatientPatientIdAndIsDeletedFalse(patientId);
        return Result.success(submissions.stream()
                .map(formSubmissionMapper::toDto)
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public Result<List<FormSubmissionDto>> getFormsBySession(UUID sessionId) {
        if (sessionId == null) {
            return Result.failure("Session ID is required");
        }

        List<FormSubmission> submissions = formSubmissionRepository.findByAppointmentSessionSessionIdAndIsDeletedFalse(sessionId);
        return Result.success(submissions.stream()
                .map(formSubmissionMapper::toDto)
                .collect(Collectors.toList()));
    }

    @Transactional
    public Result<FormSubmissionDto> attachFile(UUID submissionId, UUID fileId) {
        if (submissionId == null || fileId == null) {
            return Result.failure("Submission ID and file ID are required");
        }

        FormSubmission submission = formSubmissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            return Result.failure("Form submission not found");
        }

        try {
            File file = entityManager.getReference(File.class, fileId);
            file.getId(); // Force proxy initialization to check existence
            submission.attachFile(file);

            FormSubmission saved = formSubmissionRepository.save(submission);
            log.info("File {} attached to submission {}", fileId, submissionId);

            return Result.success(formSubmissionMapper.toDto(saved));
        } catch (Exception e) {
            return Result.failure("File not found or invalid");
        }
    }

    @Transactional(readOnly = true)
    public Result<List<FormSubmissionDto>> getExpiringSoon(int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(daysAhead);

        List<FormSubmission> submissions = formSubmissionRepository.findExpiringSoon(now, futureDate);
        return Result.success(submissions.stream()
                .map(formSubmissionMapper::toDto)
                .collect(Collectors.toList()));
    }
}
