package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.dto.ConsultationTypeFilterCriteria;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.repository.FormTemplateRepository;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.mapper.ConsultationTypeMapper;
import com.example.policlicabine.repository.ConsultationRepository;
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

    public ConsultationService(ConsultationRepository consultationRepository,
                              ConsultationTypeMapper consultationTypeMapper,
                              ConsultationTypeSpecificationBuilder specificationBuilder,
                              FormTemplateRepository formTemplateRepository) {
        super(consultationRepository, consultationTypeMapper);
        this.consultationRepository = consultationRepository;
        this.consultationTypeMapper = consultationTypeMapper;
        this.specificationBuilder = specificationBuilder;
        this.formTemplateRepository = formTemplateRepository;
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
        // Update mutable fields (NOT consultationId)
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
        // Don't update questions (managed separately)
    }

    public Result<ConsultationTypeDto> createConsultation(String name, Specialty specialty,
                                                     BigDecimal price, String priceCurrency,
                                                     Integer durationMinutes,
                                                     Boolean requiresSurgeryRoom) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return Result.failure("ConsultationType name is required");
            }

            if (specialty == null) {
                return Result.failure("Specialty is required");
            }

            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                return Result.failure("Valid price is required");
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

            return Result.success(consultationTypeMapper.toDto(savedConsultation));

        } catch (Exception e) {
            log.error("Error creating consultation", e);
            return Result.failure("Failed to create consultation: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Result<List<ConsultationTypeDto>> getAllActiveConsultations() {
        try {
            List<ConsultationType> consultations = consultationRepository.findByIsActiveTrue();

            List<ConsultationTypeDto> consultationDtos = consultations.stream()
                .map(consultationTypeMapper::toDto)
                .collect(Collectors.toList());

            return Result.success(consultationDtos);

        } catch (Exception e) {
            log.error("Error getting active consultations", e);
            return Result.failure("Failed to get consultations: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Result<List<ConsultationTypeDto>> getConsultationsBySpecialty(Specialty specialty) {
        try {
            if (specialty == null) {
                return Result.failure("Specialty is required");
            }

            List<ConsultationType> consultations = consultationRepository
                .findBySpecialty(specialty);

            List<ConsultationTypeDto> consultationDtos = consultations.stream()
                .map(consultationTypeMapper::toDto)
                .collect(Collectors.toList());

            return Result.success(consultationDtos);

        } catch (Exception e) {
            log.error("Error getting consultations by specialty", e);
            return Result.failure("Failed to get consultations: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Result<ConsultationTypeDto> findConsultationById(UUID consultationId) {
        return findById(consultationId);
    }

    public Result<ConsultationTypeDto> updatePrice(UUID consultationId, BigDecimal newPrice) {
        try {
            if (consultationId == null) {
                return Result.failure("ConsultationType ID is required");
            }

            if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
                return Result.failure("Valid price is required");
            }

            ConsultationType consultation = consultationRepository.findById(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("ConsultationType not found");
            }

            // Store old price for event
            BigDecimal oldPrice = consultation.getPrice();

            consultation.setPrice(newPrice);
            ConsultationType savedConsultation = consultationRepository.save(consultation);

            log.info("ConsultationType price updated: {} to {}", consultationId, newPrice);

            return Result.success(consultationTypeMapper.toDto(savedConsultation));

        } catch (Exception e) {
            log.error("Error updating consultation price", e);
            return Result.failure("Failed to update price: " + e.getMessage());
        }
    }

    public Result<ConsultationTypeDto> deactivateConsultation(UUID consultationId) {
        try {
            if (consultationId == null) {
                return Result.failure("ConsultationType ID is required");
            }

            ConsultationType consultation = consultationRepository.findById(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("ConsultationType not found");
            }

            if (!consultation.getIsActive()) {
                return Result.failure("ConsultationType is already inactive");
            }

            consultation.setIsActive(false);
            ConsultationType savedConsultation = consultationRepository.save(consultation);

            log.info("ConsultationType deactivated: {}", consultationId);

            return Result.success(consultationTypeMapper.toDto(savedConsultation));

        } catch (Exception e) {
            log.error("Error deactivating consultation", e);
            return Result.failure("Failed to deactivate consultation: " + e.getMessage());
        }
    }

    public Result<ConsultationTypeDto> activateConsultation(UUID consultationId) {
        try {
            if (consultationId == null) {
                return Result.failure("ConsultationType ID is required");
            }

            ConsultationType consultation = consultationRepository.findById(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("ConsultationType not found");
            }

            consultation.setIsActive(true);
            ConsultationType savedConsultation = consultationRepository.save(consultation);

            log.info("ConsultationType activated: {}", consultationId);

            return Result.success(consultationTypeMapper.toDto(savedConsultation));

        } catch (Exception e) {
            log.error("Error activating consultation", e);
            return Result.failure("Failed to activate consultation: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<ConsultationTypeDto> search(ConsultationTypeFilterCriteria criteria, Pageable pageable) {
        log.debug("Searching consultation types with criteria: {} and pageable: {}", criteria, pageable);

        try {
            Specification<ConsultationType> spec = specificationBuilder.build(criteria);
            Page<ConsultationType> entityPage = consultationRepository.findAll(spec, pageable);
            Page<ConsultationTypeDto> dtoPage = entityPage.map(this::toDto);

            log.info("ConsultationType search returned {} results (page {}/{})",
                    dtoPage.getNumberOfElements(),
                    dtoPage.getNumber() + 1,
                    dtoPage.getTotalPages());

            return dtoPage;
        } catch (Exception e) {
            log.error("Error searching consultation types with criteria: {}", criteria, e);
            throw new RuntimeException("Failed to search consultation types: " + e.getMessage(), e);
        }
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
        return consultationRepository.findByNameAndIsActiveTrue(name.trim())
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ConsultationType> getEntitiesBySpecialties(List<Specialty> specialties) {
        if (specialties == null || specialties.isEmpty()) {
            return List.of();
        }
        return consultationRepository.findBySpecialtyInAndIsActiveTrue(specialties);
    }

    // ============= FORM TEMPLATE MANAGEMENT METHODS =============

    /**
     * Sets the required form templates for a consultation type.
     * These are forms that patients must fill BEFORE the consultation.
     *
     * @param consultationId The consultation type ID
     * @param formTemplateIds List of form template IDs to assign
     * @return Result containing updated ConsultationTypeDto or error message
     */
    public Result<ConsultationTypeDto> setRequiredFormTemplates(UUID consultationId, List<UUID> formTemplateIds) {
        try {
            if (consultationId == null) {
                return Result.failure("ConsultationType ID is required");
            }

            ConsultationType consultation = consultationRepository.findWithRequiredFormTemplatesByConsultationId(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("ConsultationType not found");
            }

            Set<FormTemplate> formTemplates = new HashSet<>();
            if (formTemplateIds != null && !formTemplateIds.isEmpty()) {
                List<FormTemplate> templates = formTemplateRepository.findAllById(formTemplateIds);
                
                // Validate all templates exist and are active
                if (templates.size() != formTemplateIds.size()) {
                    return Result.failure("One or more form templates not found");
                }
                
                for (FormTemplate template : templates) {
                    if (!template.getActive() || template.getIsDeleted()) {
                        return Result.failure("Form template '" + template.getName() + "' is not active or has been deleted");
                    }
                }
                
                formTemplates.addAll(templates);
            }

            consultation.setRequiredFormTemplates(formTemplates);
            ConsultationType saved = consultationRepository.save(consultation);

            log.info("Updated required form templates for consultation {}: {} templates assigned",
                    consultationId, formTemplates.size());

            return Result.success(consultationTypeMapper.toDto(saved));

        } catch (Exception e) {
            log.error("Error setting required form templates for consultation {}", consultationId, e);
            return Result.failure("Failed to set required form templates: " + e.getMessage());
        }
    }

    /**
     * Sets the main consultation form template for a consultation type.
     * This is the form used DURING the consultation by the doctor.
     *
     * @param consultationId The consultation type ID
     * @param formTemplateId The form template ID to assign (null to clear)
     * @return Result containing updated ConsultationTypeDto or error message
     */
    public Result<ConsultationTypeDto> setConsultationFormTemplate(UUID consultationId, UUID formTemplateId) {
        try {
            if (consultationId == null) {
                return Result.failure("ConsultationType ID is required");
            }

            ConsultationType consultation = consultationRepository.findWithConsultationFormTemplateByConsultationId(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("ConsultationType not found");
            }

            FormTemplate formTemplate = null;
            if (formTemplateId != null) {
                formTemplate = formTemplateRepository.findById(formTemplateId).orElse(null);
                if (formTemplate == null) {
                    return Result.failure("FormTemplate not found");
                }
                if (!formTemplate.getActive() || formTemplate.getIsDeleted()) {
                    return Result.failure("Form template '" + formTemplate.getName() + "' is not active or has been deleted");
                }
            }

            consultation.setConsultationFormTemplate(formTemplate);
            ConsultationType saved = consultationRepository.save(consultation);

            log.info("Updated consultation form template for consultation {}: {}",
                    consultationId, formTemplate != null ? formTemplate.getName() : "cleared");

            return Result.success(consultationTypeMapper.toDto(saved));

        } catch (Exception e) {
            log.error("Error setting consultation form template for consultation {}", consultationId, e);
            return Result.failure("Failed to set consultation form template: " + e.getMessage());
        }
    }

    /**
     * Gets all required form templates for a consultation type.
     *
     * @param consultationId The consultation type ID
     * @return Result containing list of FormTemplateDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<FormTemplateDto>> getRequiredFormTemplates(UUID consultationId) {
        try {
            if (consultationId == null) {
                return Result.failure("ConsultationType ID is required");
            }

            ConsultationType consultation = consultationRepository.findWithRequiredFormTemplatesByConsultationId(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("ConsultationType not found");
            }

            List<FormTemplateDto> templates = consultation.getRequiredFormTemplates().stream()
                .filter(t -> t.getActive() && !t.getIsDeleted())
                .map(this::toFormTemplateDto)
                .collect(Collectors.toList());

            return Result.success(templates);

        } catch (Exception e) {
            log.error("Error getting required form templates for consultation {}", consultationId, e);
            return Result.failure("Failed to get required form templates: " + e.getMessage());
        }
    }

    /**
     * Gets the consultation form template for a consultation type (the form used DURING consultation).
     *
     * @param consultationId The consultation type ID
     * @return Result containing FormTemplateDto or error message (null value if no template assigned)
     */
    @Transactional(readOnly = true)
    public Result<FormTemplateDto> getConsultationFormTemplate(UUID consultationId) {
        try {
            if (consultationId == null) {
                return Result.failure("ConsultationType ID is required");
            }

            ConsultationType consultation = consultationRepository.findWithConsultationFormTemplateByConsultationId(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("ConsultationType not found");
            }

            FormTemplate template = consultation.getConsultationFormTemplate();
            if (template == null) {
                return Result.success(null);
            }

            return Result.success(toFormTemplateDto(template));

        } catch (Exception e) {
            log.error("Error getting consultation form template for consultation {}", consultationId, e);
            return Result.failure("Failed to get consultation form template: " + e.getMessage());
        }
    }

    /**
     * INTERNAL: Gets consultation entity with all form templates loaded.
     * Used by AppointmentSessionService for aggregating required forms.
     */
    @Transactional(readOnly = true)
    public ConsultationType getEntityWithFormTemplates(UUID consultationId) {
        if (consultationId == null) {
            return null;
        }
        return consultationRepository.findWithAllFormTemplatesByConsultationId(consultationId).orElse(null);
    }

    private FormTemplateDto toFormTemplateDto(FormTemplate template) {
        return FormTemplateDto.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .version(template.getVersion())
                .active(template.getActive())
                .structure(template.getStructure())
                .purpose(template.getPurpose())
                .validityMonths(template.getValidityMonths())
                .pdfTemplateUrl(template.getPdfTemplateUrl())
                .createdAt(template.getCreatedAt())
                .createdByUserId(template.getCreatedBy() != null ? template.getCreatedBy().getUserId() : null)
                .build();
    }
}
