package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.mapper.FormTemplateMapper;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.FormTemplateRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    public FormTemplateService(FormTemplateRepository repository, FormTemplateMapper mapper,
                               ApplicationEventPublisher eventPublisher, UserService userService) {
        super(repository, mapper);
        this.formTemplateRepository = repository;
        this.formTemplateMapper = mapper;
        this.eventPublisher = eventPublisher;
        this.userService = userService;
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

    @Transactional(readOnly = true)
    public FormTemplate getEntityByCode(String code) {
        return formTemplateRepository.findByCode(code).orElse(null);
    }
}
