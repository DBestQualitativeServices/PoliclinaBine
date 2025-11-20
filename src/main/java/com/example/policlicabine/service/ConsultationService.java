package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.mapper.ConsultationTypeMapper;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing ConsultationType entities.
 * Consultations are the services offered by the clinic.
 *
 * Architecture:
 * - Extends BaseServiceImpl for common CRUD operations (findById, validateExists, getEntityById)
 * - Focuses on business-specific logic (pricing, active/inactive state, specialty queries)
 * - BigDecimal for monetary amounts
 *
 * Inherited Methods (from BaseServiceImpl):
 * - findById(UUID) → Result&lt;ConsultationTypeDto&gt;
 * - validateExists(UUID) → Result&lt;Void&gt;
 * - getEntityById(UUID) → ConsultationType
 * - findAll() → Result&lt;List&lt;ConsultationTypeDto&gt;&gt;
 */
@Service
@Slf4j
@Transactional
public class ConsultationService extends BaseServiceImpl<ConsultationType, ConsultationTypeDto, UUID> {

    private final ConsultationRepository consultationRepository;
    private final ConsultationTypeMapper consultationTypeMapper;

    public ConsultationService(ConsultationRepository consultationRepository,
                              ConsultationTypeMapper consultationTypeMapper) {
        super(consultationRepository, consultationTypeMapper);
        this.consultationRepository = consultationRepository;
        this.consultationTypeMapper = consultationTypeMapper;
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

    /**
     * Creates a new consultation service.
     *
     * @param name ConsultationType name
     * @param specialty Medical specialty
     * @param price Service price
     * @param priceCurrency Currency code (e.g., "RON")
     * @param durationMinutes Expected duration
     * @param requiresSurgeryRoom Whether surgery room is needed
     * @return Result containing ConsultationTypeDto or error message
     */
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

    /**
     * Retrieves all active consultations.
     *
     * @return Result containing list of ConsultationTypeDto or error message
     */
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

    /**
     * Retrieves all consultations for a specific specialty.
     *
     * @param specialty Medical specialty
     * @return Result containing list of ConsultationTypeDto or error message
     */
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

    /**
     * Finds a consultation by its unique identifier.
     * Delegates to inherited findById() method from BaseServiceImpl.
     *
     * @param consultationId ConsultationType identifier
     * @return Result containing ConsultationTypeDto or error message
     */
    @Transactional(readOnly = true)
    public Result<ConsultationTypeDto> findConsultationById(UUID consultationId) {
        return findById(consultationId);
    }

    /**
     * Updates consultation pricing.
     *
     * @param consultationId ConsultationType identifier
     * @param newPrice New price
     * @return Result containing updated ConsultationTypeDto or error message
     */
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

    /**
     * Deactivates a consultation (soft delete).
     * Inactive consultations cannot be used for new appointments.
     *
     * @param consultationId ConsultationType identifier
     * @return Result containing updated ConsultationTypeDto or error message
     */
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

    /**
     * Reactivates a previously deactivated consultation.
     *
     * @param consultationId ConsultationType identifier
     * @return Result containing updated ConsultationTypeDto or error message
     */
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

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============
    // These methods return entities directly (not Result-wrapped DTOs) for use by other services

    /**
     * INTERNAL: Gets consultation entities by their names.
     * Used by AppointmentSessionService for validation.
     * Returns entities directly without Result wrapper for simplicity.
     *
     * @param names List of consultation names
     * @return List of ConsultationType entities (may be empty, never null)
     */
    @Transactional(readOnly = true)
    public List<ConsultationType> getEntitiesByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return consultationRepository.findByNameInAndIsActiveTrue(names);
    }

    /**
     * INTERNAL: Gets a single consultation entity by name.
     * Used by AppointmentSessionService for validation.
     * Returns entity directly, null if not found.
     *
     * @param name ConsultationType name
     * @return ConsultationType entity or null if not found
     */
    @Transactional(readOnly = true)
    public ConsultationType getEntityByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return consultationRepository.findByNameAndIsActiveTrue(name.trim())
            .orElse(null);
    }

    /**
     * INTERNAL: Gets consultation entities by their specialties.
     * Used by DoctorService to get all active consultations matching doctor's specialties.
     * Returns entities directly without Result wrapper for simplicity.
     *
     * @param specialties List of specialties
     * @return List of ConsultationType entities (may be empty, never null)
     */
    @Transactional(readOnly = true)
    public List<ConsultationType> getEntitiesBySpecialties(List<Specialty> specialties) {
        if (specialties == null || specialties.isEmpty()) {
            return List.of();
        }
        return consultationRepository.findBySpecialtyInAndIsActiveTrue(specialties);
    }
}
