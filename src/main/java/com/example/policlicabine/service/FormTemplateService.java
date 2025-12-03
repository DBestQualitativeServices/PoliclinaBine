package com.example.policlicabine.service;

import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.dto.FormTemplateFilterCriteria;
import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.event.FormTemplateCreated;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.FormTemplateMapper;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.FormTemplateRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import com.example.policlicabine.specification.FormTemplateSpecificationBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class FormTemplateService extends BaseServiceImpl<FormTemplate, FormTemplateDto, UUID> {

    private final FormTemplateRepository formTemplateRepository;
    private final FormTemplateMapper formTemplateMapper;
    private final FormTemplateSpecificationBuilder specificationBuilder;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    public FormTemplateService(FormTemplateRepository repository, FormTemplateMapper mapper,
                               FormTemplateSpecificationBuilder specificationBuilder,
                               ApplicationEventPublisher eventPublisher) {
        super(repository, mapper);
        this.formTemplateRepository = repository;
        this.formTemplateMapper = mapper;
        this.specificationBuilder = specificationBuilder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected FormTemplateDto toDto(FormTemplate entity) {
        return formTemplateMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "FormTemplate";
    }

    @Override
    protected void updateEntityFromDto(FormTemplate entity, FormTemplateDto dto) {
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            entity.setName(dto.getName().trim());
        }
        if (dto.getStructure() != null) {
            entity.setStructure(dto.getStructure());
        }
        if (dto.getValidityMonths() != null) {
            entity.setValidityMonths(dto.getValidityMonths());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FormTemplateDto findById(UUID id) {
        if (id == null) {
            throw new BusinessException("FormTemplate ID is required");
        }
        
        FormTemplate template = formTemplateRepository.findWithCreatedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FormTemplate", id));
        
        if (template.getIsDeleted()) {
            throw new ResourceNotFoundException("FormTemplate", id);
        }
        
        return formTemplateMapper.toDto(template);
    }

    @Transactional
    public FormTemplateDto createTemplate(String name, FormStructure structure,
                                          Integer validityMonths, UUID createdByUserId) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("Template name is required");
        }
        if (structure == null) {
            throw new BusinessException("Template structure is required");
        }

        if (formTemplateRepository.findByNameAndIsDeletedFalse(name.trim()).isPresent()) {
            throw new BusinessException("Template with name '" + name + "' already exists");
        }

        User createdBy = createdByUserId != null ?
                entityManager.getReference(User.class, createdByUserId) : null;

        FormTemplate template = FormTemplate.builder()
                .name(name.trim())
                .structure(structure)
                .validityMonths(validityMonths)
                .active(false)
                .createdBy(createdBy)
                .build();

        FormTemplate saved = formTemplateRepository.save(template);
        log.info("Created form template: {}", saved.getName());

        eventPublisher.publishEvent(new FormTemplateCreated(
                saved.getId(),
                saved.getName(),
                createdByUserId
        ));

        return formTemplateMapper.toDto(saved);
    }

    @Transactional
    public FormTemplateDto publishTemplate(UUID templateId) {
        if (templateId == null) {
            throw new BusinessException("Template ID is required");
        }

        FormTemplate template = formTemplateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("FormTemplate", templateId));

        if (template.getIsDeleted()) {
            throw new BusinessException("Cannot publish deleted template");
        }

        template.setActive(true);
        FormTemplate saved = formTemplateRepository.save(template);

        log.info("Published form template: {}", saved.getName());

        return formTemplateMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FormTemplateDto> getActiveTemplates() {
        List<FormTemplate> templates = formTemplateRepository.findByActiveTrueAndIsDeletedFalse();
        return templates.stream()
                .map(formTemplateMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormTemplateDto getByName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Template name is required");
        }

        FormTemplate template = formTemplateRepository.findByNameAndIsDeletedFalse(name)
            .orElseThrow(() -> new ResourceNotFoundException("FormTemplate", "name: " + name));

        return formTemplateMapper.toDto(template);
    }

    @Transactional(readOnly = true)
    public FormTemplate getEntityByName(String name) {
        return formTemplateRepository.findByNameAndIsDeletedFalse(name).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<FormTemplateDto> search(FormTemplateFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching form templates with criteria: {} and pageable: {}", criteria, pageable);

        Specification<FormTemplate> spec = specificationBuilder.build(criteria);
        Page<FormTemplate> entityPage = formTemplateRepository.findAll(spec, pageable);
        Page<FormTemplateDto> dtoPage = entityPage.map(formTemplateMapper::toDto);

        log.info("FormTemplate search returned {} results (page {}/{})",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber() + 1,
                dtoPage.getTotalPages());

        return dtoPage;
    }

    /**
     * Find template by name, including soft-deleted templates.
     * Used for hard-delete scenarios where we need to find any existing template.
     *
     * @param name the template name
     * @return Optional containing the template if found
     */
    @Transactional(readOnly = true)
    public java.util.Optional<FormTemplate> findByNameIncludingDeleted(String name) {
        if (name == null || name.trim().isEmpty()) {
            return java.util.Optional.empty();
        }
        return formTemplateRepository.findByName(name.trim());
    }

    /**
     * Hard delete a template with full cascade cleanup.
     * 1. Unlinks from all consultations (both requiredFormTemplates and consultationFormTemplate)
     * 2. Deletes all form submissions referencing this template
     * 3. Physically removes the template from the database
     *
     * @param templateId the template ID to hard delete
     * @param consultationService service for unlinking from consultations
     * @param formSubmissionService service for deleting submissions
     */
    @Transactional
    public void hardDeleteWithCascade(UUID templateId,
                                       ConsultationService consultationService,
                                       FormSubmissionService formSubmissionService) {
        if (templateId == null) {
            throw new BusinessException("Template ID is required");
        }

        FormTemplate template = formTemplateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("FormTemplate", templateId));

        log.info("Hard deleting template '{}' (ID: {}) with cascade", template.getName(), templateId);

        // 1. Unlink from all consultations
        consultationService.unlinkFormTemplateFromAll(templateId);

        // 2. Delete all submissions referencing this template
        formSubmissionService.deleteAllByTemplateId(templateId);

        // 3. Hard delete the template
        formTemplateRepository.delete(template);
        formTemplateRepository.flush();

        log.info("Template '{}' (ID: {}) hard deleted successfully", template.getName(), templateId);
    }
}
