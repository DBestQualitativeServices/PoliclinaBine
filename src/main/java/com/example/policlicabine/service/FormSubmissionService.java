package com.example.policlicabine.service;

import com.example.policlicabine.dto.FormSignatureDto;
import com.example.policlicabine.dto.FormSubmissionDto;
import com.example.policlicabine.dto.FormSubmissionFilterCriteria;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.mapper.FormSignatureMapper;
import com.example.policlicabine.repository.FormSignatureRepository;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.FormSubmissionMapper;
import com.example.policlicabine.repository.FormSubmissionRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import com.example.policlicabine.specification.FormSubmissionSpecificationBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
    private final FormSubmissionSpecificationBuilder specificationBuilder;
    private final FormSignatureRepository formSignatureRepository;
    private final FormSignatureMapper formSignatureMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public FormSubmissionService(FormSubmissionRepository repository, FormSubmissionMapper mapper,
                                  FormTemplateService formTemplateService, FormValidationService formValidationService,
                                  PatientService patientService, FormSubmissionSpecificationBuilder specificationBuilder,
                                  FormSignatureRepository formSignatureRepository, FormSignatureMapper formSignatureMapper) {
        super(repository, mapper);
        this.formSubmissionRepository = repository;
        this.formSubmissionMapper = mapper;
        this.formTemplateService = formTemplateService;
        this.formValidationService = formValidationService;
        this.patientService = patientService;
        this.specificationBuilder = specificationBuilder;
        this.formSignatureRepository = formSignatureRepository;
        this.formSignatureMapper = formSignatureMapper;
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
    }

    @Override
    @Transactional(readOnly = true)
    public FormSubmissionDto findById(UUID id) {
        if (id == null) {
            throw new BusinessException("FormSubmission ID is required");
        }
        FormSubmission submission = formSubmissionRepository.findWithDetailsById(id).orElse(null);
        if (submission == null || submission.getIsDeleted()) {
            throw new ResourceNotFoundException("FormSubmission", id);
        }
        return formSubmissionMapper.toDto(submission);
    }

    @Transactional
    public FormSubmissionDto submitForm(UUID templateId, UUID patientId, Map<String, Object> data,
                                                 UUID appointmentSessionId, UUID consultationTypeId, UUID submittedByUserId,
                                                 List<UUID> fileIds) {
        if (templateId == null) {
            throw new BusinessException("Template ID is required");
        }
        if (patientId == null) {
            throw new BusinessException("Patient ID is required");
        }
        if (data == null || data.isEmpty()) {
            throw new BusinessException("Form data is required");
        }

        FormTemplate template = formTemplateService.getEntityById(templateId);
        if (template == null) {
            throw new ResourceNotFoundException("FormTemplate", templateId);
        }

        if (!template.getActive()) {
            throw new BusinessException("Form template is not active");
        }

        patientService.validateExists(patientId);

        List<String> validationErrors = formValidationService.validate(template.getStructure(), data);
        if (!validationErrors.isEmpty()) {
            throw new BusinessException("Validation failed: " + String.join(", ", validationErrors));
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
                .submittedBy(submittedByRef)
                .expiresAt(expiresAt)
                .build();

        // Attach files if provided
        if (fileIds != null && !fileIds.isEmpty()) {
            for (UUID fileId : fileIds) {
                try {
                    File file = entityManager.getReference(File.class, fileId);
                    submission.attachFile(file);
                } catch (Exception e) {
                    log.warn("File {} not found, skipping", fileId);
                }
            }
        }

        FormSubmission saved = formSubmissionRepository.save(submission);

        // Update patient's form field cache with submitted data
        patientService.updateFormFieldCache(patientId, data);

        log.info("Form submitted: {} for patient {}", template.getName(), patientId);

        return formSubmissionMapper.toDto(saved);
    }

    @Transactional
    public FormSubmissionDto signForm(UUID submissionId, UUID witnessedByUserId) {
        if (submissionId == null) {
            throw new BusinessException("Submission ID is required");
        }
        if (witnessedByUserId == null) {
            throw new BusinessException("Witness user ID is required");
        }

        FormSubmission submission = formSubmissionRepository.findWithDetailsById(submissionId).orElse(null);
        if (submission == null) {
            throw new ResourceNotFoundException("FormSubmission", submissionId);
        }

        if (submission.getPatientSignedAt() != null) {
            throw new BusinessException("Form is already signed");
        }

        if (submission.isExpired()) {
            throw new BusinessException("Form has expired");
        }

        User witnessedBy = entityManager.getReference(User.class, witnessedByUserId);
        submission.signByPatient(witnessedBy);

        FormSubmission saved = formSubmissionRepository.save(submission);
        log.info("Form signed: {} by witness {}", submissionId, witnessedByUserId);

        return formSubmissionMapper.toDto(saved);
    }

    /**
     * Adds a signature to a form submission.
     *
     * @param submissionId the form submission ID
     * @param signatureFieldId the signature field ID (matches FormField.name)
     * @param signedByUserId the user who is signing
     * @param signatureData the base64 encoded PNG signature image
     * @return the created FormSignatureDto
     */
    @Transactional
    public FormSignatureDto addSignature(UUID submissionId, String signatureFieldId, UUID signedByUserId, String signatureData) {
        if (submissionId == null) {
            throw new BusinessException("Submission ID is required");
        }
        if (signatureFieldId == null || signatureFieldId.trim().isEmpty()) {
            throw new BusinessException("Signature field ID is required");
        }
        if (signedByUserId == null) {
            throw new BusinessException("Signed by user ID is required");
        }
        if (signatureData == null || signatureData.trim().isEmpty()) {
            throw new BusinessException("Signature data is required");
        }

        FormSubmission submission = formSubmissionRepository.findWithDetailsById(submissionId).orElse(null);
        if (submission == null || submission.getIsDeleted()) {
            throw new ResourceNotFoundException("FormSubmission", submissionId);
        }

        if (submission.isExpired()) {
            throw new BusinessException("Form has expired");
        }

        // Check if signature already exists for this field
        if (formSignatureRepository.existsByFormSubmissionIdAndSignatureFieldId(submissionId, signatureFieldId)) {
            throw new BusinessException("Signature already exists for field: " + signatureFieldId);
        }

        // Validate that signature field exists in template
        boolean fieldExists = submission.getTemplateSnapshot().getSections().stream()
                .filter(section -> section.getFields() != null)
                .flatMap(section -> section.getFields().stream())
                .anyMatch(field -> "signature".equals(field.getType()) && signatureFieldId.equals(field.getName()));

        if (!fieldExists) {
            throw new BusinessException("Signature field not found in template: " + signatureFieldId);
        }

        User signedBy = entityManager.getReference(User.class, signedByUserId);

        FormSignature signature = FormSignature.builder()
                .formSubmission(submission)
                .signatureFieldId(signatureFieldId)
                .signedBy(signedBy)
                .signatureData(Base64.getDecoder().decode(signatureData))
                .build();

        FormSignature saved = formSignatureRepository.save(signature);
        log.info("Signature added to submission {} for field {} by user {}", submissionId, signatureFieldId, signedByUserId);

        return formSignatureMapper.toDto(saved);
    }

    /**
     * Gets all signatures for a form submission.
     */
    @Transactional(readOnly = true)
    public List<FormSignatureDto> getSignatures(UUID submissionId) {
        if (submissionId == null) {
            throw new BusinessException("Submission ID is required");
        }
        List<FormSignature> signatures = formSignatureRepository.findWithSignerByFormSubmissionId(submissionId);
        return formSignatureMapper.toDtoList(signatures);
    }

    /**
     * Checks if patient has a valid submission for a specific template at a given date.
     */
    @Transactional(readOnly = true)
    public boolean hasValidSubmission(UUID patientId, UUID templateId, LocalDateTime targetDate) {
        if (patientId == null || templateId == null) {
            throw new BusinessException("Patient ID and template ID are required");
        }
        if (targetDate == null) {
            targetDate = LocalDateTime.now();
        }

        return formSubmissionRepository.existsValidSubmission(patientId, templateId, targetDate);
    }

    @Transactional(readOnly = true)
    public List<FormSubmissionDto> getFormsByPatient(UUID patientId) {
        if (patientId == null) {
            throw new BusinessException("Patient ID is required");
        }

        List<FormSubmission> submissions = formSubmissionRepository.findByPatientPatientIdAndIsDeletedFalse(patientId);
        return submissions.stream()
                .map(formSubmissionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormSubmissionDto> getFormsBySession(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        List<FormSubmission> submissions = formSubmissionRepository.findByAppointmentSessionSessionIdAndIsDeletedFalse(sessionId);
        return submissions.stream()
                .map(formSubmissionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public FormSubmissionDto attachFile(UUID submissionId, UUID fileId) {
        if (submissionId == null || fileId == null) {
            throw new BusinessException("Submission ID and file ID are required");
        }

        FormSubmission submission = formSubmissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            throw new ResourceNotFoundException("FormSubmission", submissionId);
        }

        try {
            File file = entityManager.getReference(File.class, fileId);
            file.getId(); // Force proxy initialization to check existence
            submission.attachFile(file);

            FormSubmission saved = formSubmissionRepository.save(submission);
            log.info("File {} attached to submission {}", fileId, submissionId);

            return formSubmissionMapper.toDto(saved);
        } catch (Exception e) {
            throw new BusinessException("File not found or invalid");
        }
    }

    @Transactional(readOnly = true)
    public List<FormSubmissionDto> getExpiringSoon(int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(daysAhead);

        List<FormSubmission> submissions = formSubmissionRepository.findExpiringSoon(now, futureDate);
        return submissions.stream()
                .map(formSubmissionMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Search form submissions with dynamic filter criteria.
     *
     * @param criteria the filter criteria (all fields optional)
     * @param pageable pagination information
     * @return paginated list of matching form submissions
     */
    @Transactional(readOnly = true)
    public Page<FormSubmissionDto> search(FormSubmissionFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching form submissions with criteria: {} and pageable: {}", criteria, pageable);

        Specification<FormSubmission> spec = specificationBuilder.build(criteria)
                .and((root, query, cb) -> cb.equal(root.get("isDeleted"), false));

        Page<FormSubmission> entityPage = formSubmissionRepository.findAll(spec, pageable);

        Page<FormSubmissionDto> dtoPage = entityPage.map(formSubmissionMapper::toDto);

        log.info("Form submission search returned {} results (page {}/{})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber() + 1,
                dtoPage.getTotalPages());

        return dtoPage;
    }

    /**
     * Delete all form submissions that reference a specific template.
     * Used during template hard-delete cascade.
     *
     * @param templateId the template ID whose submissions should be deleted
     */
    @Transactional
    public void deleteAllByTemplateId(UUID templateId) {
        if (templateId == null) {
            throw new BusinessException("Template ID is required");
        }
        log.info("Deleting all form submissions for template: {}", templateId);
        formSubmissionRepository.deleteByTemplateId(templateId);
        formSubmissionRepository.flush();
        log.info("Form submissions deleted for template: {}", templateId);
    }
}
