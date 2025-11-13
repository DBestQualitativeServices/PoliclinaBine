package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ConsultationDto;
import com.example.policlicabine.entity.Consultation;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.event.ConsultationActivated;
import com.example.policlicabine.event.ConsultationCreated;
import com.example.policlicabine.event.ConsultationDeactivated;
import com.example.policlicabine.event.ConsultationPriceUpdated;
import com.example.policlicabine.mapper.ConsultationMapper;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Consultation entities.
 * Consultations are the services offered by the clinic.
 *
 * Architecture:
 * - Extends BaseServiceImpl for common CRUD operations (findById, validateExists, getEntityById)
 * - Focuses on business-specific logic (pricing, active/inactive state, specialty queries)
 * - BigDecimal for monetary amounts
 *
 * Inherited Methods (from BaseServiceImpl):
 * - findById(UUID) → Result&lt;ConsultationDto&gt;
 * - validateExists(UUID) → Result&lt;Void&gt;
 * - getEntityById(UUID) → Consultation
 * - findAll() → Result&lt;List&lt;ConsultationDto&gt;&gt;
 */
@Service
@Slf4j
@Transactional
public class ConsultationService extends BaseServiceImpl<Consultation, ConsultationDto, UUID> {

    private final ConsultationRepository consultationRepository;
    private final ConsultationMapper consultationMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ConsultationService(ConsultationRepository consultationRepository,
                              ConsultationMapper consultationMapper,
                              ApplicationEventPublisher eventPublisher) {
        super(consultationRepository, consultationMapper);
        this.consultationRepository = consultationRepository;
        this.consultationMapper = consultationMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected ConsultationDto toDto(Consultation entity) {
        return consultationMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Consultation";
    }

    @Override
    protected void updateEntityFromDto(Consultation entity, ConsultationDto dto) {
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
     * @param name Consultation name
     * @param specialty Medical specialty
     * @param price Service price
     * @param priceCurrency Currency code (e.g., "RON")
     * @param durationMinutes Expected duration
     * @param requiresSurgeryRoom Whether surgery room is needed
     * @return Result containing ConsultationDto or error message
     */
    public Result<ConsultationDto> createConsultation(String name, Specialty specialty,
                                                     BigDecimal price, String priceCurrency,
                                                     Integer durationMinutes,
                                                     Boolean requiresSurgeryRoom) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return Result.failure("Consultation name is required");
            }

            if (specialty == null) {
                return Result.failure("Specialty is required");
            }

            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                return Result.failure("Valid price is required");
            }

            Consultation consultation = Consultation.builder()
                .name(name.trim())
                .specialty(specialty)
                .price(price)
                .priceCurrency(priceCurrency != null ? priceCurrency : "RON")
                .durationMinutes(durationMinutes)
                .requiresSurgeryRoom(requiresSurgeryRoom != null ? requiresSurgeryRoom : false)
                .isActive(true)
                .build();

            Consultation savedConsultation = consultationRepository.save(consultation);

            // Publish domain event
            eventPublisher.publishEvent(new ConsultationCreated(
                savedConsultation.getConsultationId(),
                savedConsultation.getName(),
                savedConsultation.getSpecialty(),
                savedConsultation.getPrice(),
                savedConsultation.getPriceCurrency(),
                savedConsultation.getDurationMinutes(),
                savedConsultation.getRequiresSurgeryRoom()
            ));

            log.info("Consultation created: {} - {}", savedConsultation.getConsultationId(), name);

            return Result.success(consultationMapper.toDto(savedConsultation));

        } catch (Exception e) {
            log.error("Error creating consultation", e);
            return Result.failure("Failed to create consultation: " + e.getMessage());
        }
    }

    /**
     * Retrieves all active consultations.
     *
     * @return Result containing list of ConsultationDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<ConsultationDto>> getAllActiveConsultations() {
        try {
            List<Consultation> consultations = consultationRepository.findByIsActiveTrue();

            List<ConsultationDto> consultationDtos = consultations.stream()
                .map(consultationMapper::toDto)
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
     * @return Result containing list of ConsultationDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<ConsultationDto>> getConsultationsBySpecialty(Specialty specialty) {
        try {
            if (specialty == null) {
                return Result.failure("Specialty is required");
            }

            List<Consultation> consultations = consultationRepository
                .findBySpecialty(specialty);

            List<ConsultationDto> consultationDtos = consultations.stream()
                .map(consultationMapper::toDto)
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
     * @param consultationId Consultation identifier
     * @return Result containing ConsultationDto or error message
     */
    @Transactional(readOnly = true)
    public Result<ConsultationDto> findConsultationById(UUID consultationId) {
        return findById(consultationId);
    }

    /**
     * Updates consultation pricing.
     *
     * @param consultationId Consultation identifier
     * @param newPrice New price
     * @return Result containing updated ConsultationDto or error message
     */
    public Result<ConsultationDto> updatePrice(UUID consultationId, BigDecimal newPrice) {
        try {
            if (consultationId == null) {
                return Result.failure("Consultation ID is required");
            }

            if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
                return Result.failure("Valid price is required");
            }

            Consultation consultation = consultationRepository.findById(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("Consultation not found");
            }

            // Store old price for event
            BigDecimal oldPrice = consultation.getPrice();

            consultation.setPrice(newPrice);
            Consultation savedConsultation = consultationRepository.save(consultation);

            // Publish domain event
            eventPublisher.publishEvent(new ConsultationPriceUpdated(
                consultationId,
                consultation.getName(),
                oldPrice,
                newPrice
            ));

            log.info("Consultation price updated: {} to {}", consultationId, newPrice);

            return Result.success(consultationMapper.toDto(savedConsultation));

        } catch (Exception e) {
            log.error("Error updating consultation price", e);
            return Result.failure("Failed to update price: " + e.getMessage());
        }
    }

    /**
     * Deactivates a consultation (soft delete).
     * Inactive consultations cannot be used for new appointments.
     *
     * @param consultationId Consultation identifier
     * @return Result containing updated ConsultationDto or error message
     */
    public Result<ConsultationDto> deactivateConsultation(UUID consultationId) {
        try {
            if (consultationId == null) {
                return Result.failure("Consultation ID is required");
            }

            Consultation consultation = consultationRepository.findById(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("Consultation not found");
            }

            if (!consultation.getIsActive()) {
                return Result.failure("Consultation is already inactive");
            }

            consultation.setIsActive(false);
            Consultation savedConsultation = consultationRepository.save(consultation);

            // Publish domain event
            eventPublisher.publishEvent(new ConsultationDeactivated(
                consultationId,
                consultation.getName()
            ));

            log.info("Consultation deactivated: {}", consultationId);

            return Result.success(consultationMapper.toDto(savedConsultation));

        } catch (Exception e) {
            log.error("Error deactivating consultation", e);
            return Result.failure("Failed to deactivate consultation: " + e.getMessage());
        }
    }

    /**
     * Reactivates a previously deactivated consultation.
     *
     * @param consultationId Consultation identifier
     * @return Result containing updated ConsultationDto or error message
     */
    public Result<ConsultationDto> activateConsultation(UUID consultationId) {
        try {
            if (consultationId == null) {
                return Result.failure("Consultation ID is required");
            }

            Consultation consultation = consultationRepository.findById(consultationId)
                .orElse(null);
            if (consultation == null) {
                return Result.failure("Consultation not found");
            }

            consultation.setIsActive(true);
            Consultation savedConsultation = consultationRepository.save(consultation);

            // Publish domain event
            eventPublisher.publishEvent(new ConsultationActivated(
                consultationId,
                consultation.getName()
            ));

            log.info("Consultation activated: {}", consultationId);

            return Result.success(consultationMapper.toDto(savedConsultation));

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
     * @return List of Consultation entities (may be empty, never null)
     */
    @Transactional(readOnly = true)
    public List<Consultation> getEntitiesByNames(List<String> names) {
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
     * @param name Consultation name
     * @return Consultation entity or null if not found
     */
    @Transactional(readOnly = true)
    public Consultation getEntityByName(String name) {
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
     * @return List of Consultation entities (may be empty, never null)
     */
    @Transactional(readOnly = true)
    public List<Consultation> getEntitiesBySpecialties(List<Specialty> specialties) {
        if (specialties == null || specialties.isEmpty()) {
            return List.of();
        }
        return consultationRepository.findBySpecialtyInAndIsActiveTrue(specialties);
    }
}
