package com.example.policlicabine.service;

import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.dto.ConsultationTypeFilterCriteria;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.ConsultationTypeMapper;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.repository.FormTemplateRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import com.example.policlicabine.specification.ConsultationTypeSpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class ConsultationService extends BaseServiceImpl<ConsultationType, ConsultationTypeDto, UUID> {

    private final ConsultationRepository consultationRepository;
    private final ConsultationTypeMapper consultationTypeMapper;
    private final ConsultationTypeSpecificationBuilder specificationBuilder;
    private final FormTemplateRepository formTemplateRepository;
    private final com.example.policlicabine.mapper.FormTemplateMapper formTemplateMapper;

    public ConsultationService(ConsultationRepository consultationRepository,
                              ConsultationTypeMapper consultationTypeMapper,
                              ConsultationTypeSpecificationBuilder specificationBuilder,
                              FormTemplateRepository formTemplateRepository,
                              com.example.policlicabine.mapper.FormTemplateMapper formTemplateMapper) {
        super(consultationRepository, consultationTypeMapper);
        this.consultationRepository = consultationRepository;
        this.consultationTypeMapper = consultationTypeMapper;
        this.specificationBuilder = specificationBuilder;
        this.formTemplateRepository = formTemplateRepository;
        this.formTemplateMapper = formTemplateMapper;
    }

    @Override
    protected ConsultationTypeDto toDto(ConsultationType entity) {
        return consultationTypeMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "ConsultationType";
    }

    @Override
    protected void updateEntityFromDto(ConsultationType entity, ConsultationTypeDto dto) {
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            entity.setName(dto.getName().trim());
        }
        if (dto.getSpecialty() != null) {
            entity.setSpecialty(dto.getSpecialty());
        }
        if (dto.getPrice() != null) {
            entity.setPrice(dto.getPrice());
        }
        if (dto.getPriceCurrency() != null) {
            entity.setPriceCurrency(dto.getPriceCurrency());
        }
        if (dto.getDurationMinutes() != null) {
            entity.setDurationMinutes(dto.getDurationMinutes());
        }
        if (dto.getRequiresSurgeryRoom() != null) {
            entity.setRequiresSurgeryRoom(dto.getRequiresSurgeryRoom());
        }
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
        if (dto.getWorkflowStep() != null) {
            entity.setWorkflowStep(dto.getWorkflowStep());
        }
        if (dto.getCategoryLevel1() != null) {
            entity.setCategoryLevel1(dto.getCategoryLevel1());
        }
        if (dto.getCategoryLevel2() != null) {
            entity.setCategoryLevel2(dto.getCategoryLevel2());
        }
        if (dto.getSubcategoryLevel1() != null) {
            entity.setSubcategoryLevel1(dto.getSubcategoryLevel1());
        }
        if (dto.getSubcategoryLevel2() != null) {
            entity.setSubcategoryLevel2(dto.getSubcategoryLevel2());
        }
        if (dto.getCategoryPath() != null) {
            entity.setCategoryPath(dto.getCategoryPath());
        }
    }

    public ConsultationTypeDto createConsultation(String name, Specialty specialty,
                                                  BigDecimal price, String priceCurrency,
                                                  Integer durationMinutes,
                                                  Boolean requiresSurgeryRoom) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("ConsultationType name is required");
        }
        if (specialty == null) {
            throw new BusinessException("Specialty is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Valid price is required");
        }

        ConsultationType consultation = ConsultationType.builder()
            .name(name.trim())
            .specialty(specialty)
            .price(price)
            .priceCurrency(priceCurrency != null ? priceCurrency : "RON")
            .durationMinutes(durationMinutes)
            .requiresSurgeryRoom(requiresSurgeryRoom != null ? requiresSurgeryRoom : false)
            .isActive(true)
            .build();

        ConsultationType savedConsultation = consultationRepository.save(consultation);

        log.info("ConsultationType created: {} - {}", savedConsultation.getConsultationId(), name);

        return consultationTypeMapper.toDto(savedConsultation);
    }

    @Transactional(readOnly = true)
    public List<ConsultationTypeDto> getAllActiveConsultations() {
        List<ConsultationType> consultations = consultationRepository.findByIsActiveTrue();

        return consultations.stream()
            .map(consultationTypeMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConsultationTypeDto> getConsultationsBySpecialty(Specialty specialty) {
        if (specialty == null) {
            throw new BusinessException("Specialty is required");
        }

        List<ConsultationType> consultations = consultationRepository.findBySpecialty(specialty);

        return consultations.stream()
            .map(consultationTypeMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsultationTypeDto findConsultationById(UUID consultationId) {
        return findById(consultationId);
    }

    public ConsultationTypeDto updatePrice(UUID consultationId, BigDecimal newPrice) {
        if (consultationId == null) {
            throw new BusinessException("ConsultationType ID is required");
        }
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Valid price is required");
        }

        ConsultationType consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationType", consultationId));

        consultation.setPrice(newPrice);
        ConsultationType savedConsultation = consultationRepository.save(consultation);

        log.info("ConsultationType price updated: {} to {}", consultationId, newPrice);

        return consultationTypeMapper.toDto(savedConsultation);
    }

    public ConsultationTypeDto deactivateConsultation(UUID consultationId) {
        if (consultationId == null) {
            throw new BusinessException("ConsultationType ID is required");
        }

        ConsultationType consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationType", consultationId));

        if (!consultation.getIsActive()) {
            throw new BusinessException("ConsultationType is already inactive");
        }

        consultation.setIsActive(false);
        ConsultationType savedConsultation = consultationRepository.save(consultation);

        log.info("ConsultationType deactivated: {}", consultationId);

        return consultationTypeMapper.toDto(savedConsultation);
    }

    public ConsultationTypeDto activateConsultation(UUID consultationId) {
        if (consultationId == null) {
            throw new BusinessException("ConsultationType ID is required");
        }

        ConsultationType consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationType", consultationId));

        consultation.setIsActive(true);
        ConsultationType savedConsultation = consultationRepository.save(consultation);

        log.info("ConsultationType activated: {}", consultationId);

        return consultationTypeMapper.toDto(savedConsultation);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationTypeDto> search(ConsultationTypeFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching consultation types with criteria: {} and pageable: {}", criteria, pageable);

        Specification<ConsultationType> spec = specificationBuilder.build(criteria);
        Page<ConsultationType> entityPage = consultationRepository.findAll(spec, pageable);
        Page<ConsultationTypeDto> dtoPage = entityPage.map(this::toDto);

        log.info("ConsultationType search returned {} results (page {}/{})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber() + 1,
                dtoPage.getTotalPages());

        return dtoPage;
    }

    @Transactional(readOnly = true)
    public List<ConsultationType> getEntitiesByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return consultationRepository.findByNameInAndIsActiveTrue(names);
    }

    @Transactional(readOnly = true)
    public ConsultationType getEntityByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return consultationRepository.findByNameAndIsActiveTrue(name.trim()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ConsultationType> getEntitiesBySpecialties(List<Specialty> specialties) {
        if (specialties == null || specialties.isEmpty()) {
            return List.of();
        }
        return consultationRepository.findBySpecialtyInAndIsActiveTrue(specialties);
    }

    // ============= FORM TEMPLATE MANAGEMENT METHODS =============

    public ConsultationTypeDto setRequiredFormTemplates(UUID consultationId, List<UUID> formTemplateIds) {
        if (consultationId == null) {
            throw new BusinessException("ConsultationType ID is required");
        }

        ConsultationType consultation = consultationRepository.findWithRequiredFormTemplatesByConsultationId(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationType", consultationId));

        Set<FormTemplate> formTemplates = new HashSet<>();
        if (formTemplateIds != null && !formTemplateIds.isEmpty()) {
            List<FormTemplate> templates = formTemplateRepository.findAllById(formTemplateIds);

            if (templates.size() != formTemplateIds.size()) {
                throw new BusinessException("One or more form templates not found");
            }

            for (FormTemplate template : templates) {
                if (!template.getActive() || template.getIsDeleted()) {
                    throw new BusinessException("Form template '" + template.getName() + "' is not active or has been deleted");
                }
            }

            formTemplates.addAll(templates);
        }

        consultation.setRequiredFormTemplates(formTemplates);
        ConsultationType saved = consultationRepository.save(consultation);

        log.info("Updated required form templates for consultation {}: {} templates assigned",
                consultationId, formTemplates.size());

        return consultationTypeMapper.toDto(saved);
    }

    public ConsultationTypeDto setConsultationFormTemplate(UUID consultationId, UUID formTemplateId) {
        if (consultationId == null) {
            throw new BusinessException("ConsultationType ID is required");
        }

        ConsultationType consultation = consultationRepository.findWithConsultationFormTemplateByConsultationId(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationType", consultationId));

        FormTemplate formTemplate = null;
        if (formTemplateId != null) {
            formTemplate = formTemplateRepository.findById(formTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("FormTemplate", formTemplateId));
            if (!formTemplate.getActive() || formTemplate.getIsDeleted()) {
                throw new BusinessException("Form template '" + formTemplate.getName() + "' is not active or has been deleted");
            }
        }

        consultation.setConsultationFormTemplate(formTemplate);
        ConsultationType saved = consultationRepository.save(consultation);

        log.info("Updated consultation form template for consultation {}: {}",
                consultationId, formTemplate != null ? formTemplate.getName() : "cleared");

        return consultationTypeMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FormTemplateDto> getRequiredFormTemplates(UUID consultationId) {
        if (consultationId == null) {
            throw new BusinessException("ConsultationType ID is required");
        }

        ConsultationType consultation = consultationRepository.findWithRequiredFormTemplatesByConsultationId(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationType", consultationId));

        return consultation.getRequiredFormTemplates().stream()
            .filter(t -> t.getActive() && !t.getIsDeleted())
            .map(formTemplateMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormTemplateDto getConsultationFormTemplate(UUID consultationId) {
        if (consultationId == null) {
            throw new BusinessException("ConsultationType ID is required");
        }

        ConsultationType consultation = consultationRepository.findWithConsultationFormTemplateByConsultationId(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("ConsultationType", consultationId));

        FormTemplate template = consultation.getConsultationFormTemplate();
        if (template == null) {
            return null;
        }

        return formTemplateMapper.toDto(template);
    }

    @Transactional(readOnly = true)
    public ConsultationType getEntityWithFormTemplates(UUID consultationId) {
        if (consultationId == null) {
            return null;
        }
        return consultationRepository.findWithAllFormTemplatesByConsultationId(consultationId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ConsultationType> getEntitiesWithFormTemplatesByIds(List<UUID> consultationIds) {
        if (consultationIds == null || consultationIds.isEmpty()) {
            return List.of();
        }
        return consultationRepository.findWithRequiredFormTemplatesByConsultationIdIn(consultationIds);
    }

    /**
     * Get all consultations with their required form templates eagerly loaded.
     * Used for bulk operations like unlinking form templates.
     */
    @Transactional(readOnly = true)
    public List<ConsultationType> findAllWithRequiredFormTemplates() {
        return consultationRepository.findAllWithRequiredFormTemplates();
    }

    /**
     * Unlink a form template from all consultation types.
     * Used during template hard-delete cascade.
     *
     * @param formTemplateId the form template ID to unlink
     */
    @Transactional
    public void unlinkFormTemplateFromAll(UUID formTemplateId) {
        if (formTemplateId == null) {
            throw new BusinessException("Form template ID is required");
        }

        List<ConsultationType> consultations = findAllWithRequiredFormTemplates();

        for (ConsultationType consultation : consultations) {
            boolean removed = consultation.getRequiredFormTemplates()
                .removeIf(ft -> ft.getId().equals(formTemplateId));

            if (removed) {
                log.info("Unlinked form template {} from consultation: {}",
                         formTemplateId, consultation.getName());
                consultationRepository.save(consultation);
            }

            // Also clear consultationFormTemplate if it matches
            if (consultation.getConsultationFormTemplate() != null &&
                consultation.getConsultationFormTemplate().getId().equals(formTemplateId)) {
                consultation.setConsultationFormTemplate(null);
                log.info("Cleared consultation form template {} from consultation: {}",
                         formTemplateId, consultation.getName());
                consultationRepository.save(consultation);
            }
        }

        consultationRepository.flush();
        log.info("Form template {} unlinked from all consultations", formTemplateId);
    }
}
