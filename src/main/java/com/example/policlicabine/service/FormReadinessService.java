package com.example.policlicabine.service;

import com.example.policlicabine.dto.FormReadinessDto;
import com.example.policlicabine.dto.FormRequirementDto;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.FormSubmission;
import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.entity.enums.FormRequirementStatus;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.repository.FormSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormReadinessService {

    private final ConsultationRepository consultationRepository;
    private final FormSubmissionRepository formSubmissionRepository;

    @Transactional(readOnly = true)
    public FormReadinessDto checkReadiness(UUID patientId, List<UUID> consultationTypeIds, LocalDateTime appointmentDate) {
        if (patientId == null) {
            throw new BusinessException("Patient ID is required");
        }
        if (appointmentDate == null) {
            throw new BusinessException("Appointment date is required");
        }

        log.debug("Checking form readiness for patient {} with {} consultation types",
                patientId, consultationTypeIds != null ? consultationTypeIds.size() : 0);

        if (consultationTypeIds == null || consultationTypeIds.isEmpty()) {
            return buildEmptyReadiness(patientId, appointmentDate);
        }

        List<ConsultationType> types = consultationRepository.findWithRequiredFormTemplatesByConsultationIdIn(consultationTypeIds);

        Set<FormTemplate> requiredTemplates = new HashSet<>();
        for (ConsultationType ct : types) {
            for (FormTemplate ft : ct.getRequiredFormTemplates()) {
                if (ft.getActive() && !ft.getIsDeleted()) {
                    requiredTemplates.add(ft);
                }
            }
        }

        if (requiredTemplates.isEmpty()) {
            return buildEmptyReadiness(patientId, appointmentDate);
        }

        List<FormRequirementDto> requirements = new ArrayList<>();
        int validCount = 0, missingCount = 0, expiredCount = 0, pendingSignatureCount = 0;

        for (FormTemplate template : requiredTemplates) {
            FormRequirementDto req = checkSingleRequirement(patientId, template, appointmentDate);
            requirements.add(req);

            switch (req.getStatus()) {
                case VALID -> validCount++;
                case MISSING -> missingCount++;
                case EXPIRED -> expiredCount++;
                case PENDING_SIGNATURE -> pendingSignatureCount++;
            }
        }

        log.info("Form readiness check: patient={}, required={}, valid={}, missing={}",
                patientId, requirements.size(), validCount, missingCount);

        return FormReadinessDto.builder()
                .patientId(patientId)
                .appointmentDate(appointmentDate)
                .totalRequired(requirements.size())
                .validCount(validCount)
                .missingCount(missingCount)
                .expiredCount(expiredCount)
                .pendingSignatureCount(pendingSignatureCount)
                .allFormsComplete(validCount == requirements.size())
                .requirements(requirements)
                .build();
    }

    private FormRequirementDto checkSingleRequirement(UUID patientId, FormTemplate template, LocalDateTime appointmentDate) {
        Optional<FormSubmission> validSubmission = formSubmissionRepository
                .findValidSubmissionForTemplate(patientId, template.getId(), appointmentDate);

        if (validSubmission.isPresent()) {
            FormSubmission fs = validSubmission.get();
            return FormRequirementDto.builder()
                    .templateId(template.getId())
                    .templateName(template.getName())
                    .status(FormRequirementStatus.VALID)
                    .existingSubmissionId(fs.getId())
                    .expiresAt(fs.getExpiresAt())
                    .build();
        }

        FormRequirementStatus failureReason = determineFailureReason(patientId, template.getId(), appointmentDate);

        return FormRequirementDto.builder()
                .templateId(template.getId())
                .templateName(template.getName())
                .status(failureReason)
                .build();
    }

    private FormRequirementStatus determineFailureReason(UUID patientId, UUID templateId, LocalDateTime appointmentDate) {
        List<FormSubmission> submissions = formSubmissionRepository.findByPatientPatientIdAndTemplateIdAndIsDeletedFalse(patientId, templateId);

        if (submissions.isEmpty()) {
            return FormRequirementStatus.MISSING;
        }

        for (FormSubmission fs : submissions) {
            if (fs.getExpiresAt() != null && appointmentDate.isAfter(fs.getExpiresAt())) {
                return FormRequirementStatus.EXPIRED;
            }
        }

        return FormRequirementStatus.MISSING;
    }

    private FormReadinessDto buildEmptyReadiness(UUID patientId, LocalDateTime appointmentDate) {
        return FormReadinessDto.builder()
                .patientId(patientId)
                .appointmentDate(appointmentDate)
                .totalRequired(0)
                .validCount(0)
                .missingCount(0)
                .expiredCount(0)
                .pendingSignatureCount(0)
                .allFormsComplete(true)
                .requirements(Collections.emptyList())
                .build();
    }
}
