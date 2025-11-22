package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.dto.FormTemplateFilterCriteria;
import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.mapper.FormTemplateMapper;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.FormTemplateRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import com.example.policlicabine.specification.FormTemplateSpecificationBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
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

    @PersistenceContext
    private EntityManager entityManager;

    public FormTemplateService(FormTemplateRepository repository, FormTemplateMapper mapper,
                               FormTemplateSpecificationBuilder specificationBuilder) {
        super(repository, mapper);
        this.formTemplateRepository = repository;
        this.formTemplateMapper = mapper;

        this.specificationBuilder = specificationBuilder;
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
        boolean hasChanges = false;

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            entity.setName(dto.getName().trim());
            hasChanges = true;
        }
        if (dto.getStructure() != null) {
            entity.setStructure(dto.getStructure());
            hasChanges = true;
        }
        if (dto.getValidityMonths() != null) {
            entity.setValidityMonths(dto.getValidityMonths());
            hasChanges = true;
        }

        if (hasChanges) {
            entity.setVersion(entity.getVersion() + 1);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result<FormTemplateDto> findById(UUID id) {
        FormTemplate template = formTemplateRepository.findById(id).orElse(null);
        if (template == null || template.getIsDeleted()) {
            return Result.failure("FormTemplate not found");
        }
        return Result.success(formTemplateMapper.toDto(template));
    }

    @Transactional
    public Result<FormTemplateDto> createTemplate(String code, String name, FormStructure structure,
                                                   FormPurpose purpose, Integer validityMonths, UUID createdByUserId) {
        if (code == null || code.trim().isEmpty()) {
            return Result.failure("Template code is required");
        }
        if (name == null || name.trim().isEmpty()) {
            return Result.failure("Template name is required");
        }
        if (structure == null) {
            return Result.failure("Template structure is required");
        }
        if (purpose == null) {
            return Result.failure("Template purpose is required");
        }

        if (formTemplateRepository.findByCode(code).isPresent()) {
            return Result.failure("Template with code " + code + " already exists");
        }

        User createdBy = createdByUserId != null ?
                entityManager.getReference(User.class, createdByUserId) : null;

        FormTemplate template = FormTemplate.builder()
                .code(code.trim())
                .name(name.trim())
                .structure(structure)
                .purpose(purpose)
                .validityMonths(validityMonths)
                .active(false)
                .createdBy(createdBy)
                .build();

        FormTemplate saved = formTemplateRepository.save(template);
        log.info("Created form template: {} ({})", saved.getName(), saved.getCode());

        return Result.success(formTemplateMapper.toDto(saved));
    }

    @Transactional
    public Result<FormTemplateDto> publishTemplate(UUID templateId) {
        FormTemplate template = formTemplateRepository.findById(templateId).orElse(null);
        if (template == null) {
            return Result.failure("FormTemplate not found with id: " + templateId);
        }

        if (template.getIsDeleted()) {
            return Result.failure("Cannot publish deleted template");
        }

        template.setActive(true);
        FormTemplate saved = formTemplateRepository.save(template);
        log.info("Published form template: {} ({})", saved.getName(), saved.getCode());

        return Result.success(formTemplateMapper.toDto(saved));
    }

    @Transactional(readOnly = true)
    public Result<FormTemplateDto> getLatestTemplateByPurpose(FormPurpose purpose) {
        if (purpose == null) {
            return Result.failure("Purpose is required");
        }

        FormTemplate template = formTemplateRepository.findLatestByPurpose(purpose.name()).orElse(null);
        if (template == null) {
            return Result.failure("No active template found for purpose: " + purpose);
        }

        return Result.success(formTemplateMapper.toDto(template));
    }

    @Transactional(readOnly = true)
    public Result<List<FormTemplateDto>> getActiveTemplates() {
        List<FormTemplate> templates = formTemplateRepository.findByActiveTrueAndIsDeletedFalse();
        return Result.success(templates.stream()
                .map(formTemplateMapper::toDto)
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public Result<List<FormTemplateDto>> getTemplatesByPurpose(FormPurpose purpose) {
        if (purpose == null) {
            return Result.failure("Purpose is required");
        }

        List<FormTemplate> templates = formTemplateRepository.findByPurposeAndActiveTrueAndIsDeletedFalse(purpose);
        return Result.success(templates.stream()
                .map(formTemplateMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Searches form templates with dynamic filtering, pagination, and sorting.
     *
     * This method uses JPA Specifications for building dynamic queries based on
     * the provided filter criteria. All filters are optional and combined with AND logic.
     *
     * Supported filters:
     * - code, name - partial match, case-insensitive
     * - purpose - exact match (enum)
     * - active, isDeleted - boolean filters
     * - createdAfter, createdBefore - date range filtering
     * - createdByUserId - filter by creator user
     *
     * @param criteria filter criteria (all fields optional)
     * @param pageable pagination and sorting parameters
     * @return Page of FormTemplateDto with pagination metadata
     */
    @Transactional(readOnly = true)
    public Page<FormTemplateDto> search(FormTemplateFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching form templates with criteria: {} and pageable: {}", criteria, pageable);

        try {
            // Build dynamic specification from filter criteria
            Specification<FormTemplate> spec = specificationBuilder.build(criteria);

            // Execute query with pagination
            Page<FormTemplate> entityPage = formTemplateRepository.findAll(spec, pageable);

            // Map entities to DTOs using Spring's Page.map()
            Page<FormTemplateDto> dtoPage = entityPage.map(formTemplateMapper::toDto);

            log.info("FormTemplate search returned {} results (page {}/{})",
                    dtoPage.getNumberOfElements(),
                    dtoPage.getNumber() + 1,
                    dtoPage.getTotalPages());

            return dtoPage;

        } catch (Exception e) {
            log.error("Error searching form templates with criteria: {}", criteria, e);
            return Page.empty();
        }
    }

    @Transactional(readOnly = true)
    public FormTemplate getEntityByCode(String code) {
        return formTemplateRepository.findByCode(code).orElse(null);
    }
}
