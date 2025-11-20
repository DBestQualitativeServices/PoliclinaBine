package com.example.policlicabine.service.base;

import com.example.policlicabine.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Abstract base service implementation providing common CRUD operations.
 *
 * This class implements the generic patterns used across all services:
 * - Standard findById, validateExists, getEntityById methods
 * - Consistent error handling with try-catch and logging
 * - Transaction management (readOnly for queries)
 * - Result pattern for error propagation
 *
 * Type Parameters:
 * @param <E> Entity type
 * @param <D> DTO type
 * @param <ID> ID type (typically UUID)
 *
 * Required Subclass Implementation:
 * Subclasses must provide:
 * 1. Constructor calling super(repository, mapper)
 * 2. Implementation of toDto(E entity) method
 * 3. Implementation of getEntityName() for error messages
 *
 * Example:
 * <pre>
 * {@code
 * @Service
 * @Slf4j
 * public class PatientService extends BaseServiceImpl<Patient, PatientDto, UUID> {
 *
 *     private final PatientMapper patientMapper;
 *
 *     public PatientService(PatientRepository repository, PatientMapper mapper) {
 *         super(repository, mapper);
 *         this.patientMapper = mapper;
 *     }
 *
 *     @Override
 *     protected PatientDto toDto(Patient entity) {
 *         return patientMapper.toDto(entity);
 *     }
 *
 *     @Override
 *     protected String getEntityName() {
 *         return "Patient";
 *     }
 *
 *     // Business-specific methods
 *     public Result<PatientDto> registerNewPatient(...) { ... }
 * }
 * }
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
public abstract class BaseServiceImpl<E, D, ID> implements BaseService<E, D, ID> {

    /**
     * JPA repository for the entity.
     * Injected by constructor in subclasses.
     */
    protected final JpaRepository<E, ID> repository;

    /**
     * Mapper for entity-to-DTO conversion.
     * Note: Subclasses should implement toDto() method to use this.
     */
    protected final Object mapper;

    // ============= PUBLIC API METHODS (Return Result<DTO>) =============

    /**
     * {@inheritDoc}
     *
     * Implementation:
     * 1. Validates ID is not null
     * 2. Queries repository
     * 3. Converts to DTO
     * 4. Returns Result with proper error handling
     */
    @Override
    @Transactional(readOnly = true)
    public Result<D> findById(ID id) {
        try {
            if (id == null) {
                return Result.failure(getEntityName() + " ID is required");
            }

            E entity = repository.findById(id).orElse(null);
            if (entity == null) {
                return Result.failure(getEntityName() + " not found");
            }

            return Result.success(toDto(entity));

        } catch (Exception e) {
            log.error("Error finding {} by ID", getEntityName(), e);
            return Result.failure("Failed to find " + getEntityName() + ": " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * Implementation:
     * 1. Queries all entities from repository
     * 2. Converts each to DTO
     * 3. Returns Result with list
     *
     * Note: Use with caution for large datasets.
     */
    @Override
    @Transactional(readOnly = true)
    public Result<List<D>> findAll() {
        try {
            List<E> entities = repository.findAll();

            List<D> dtos = entities.stream()
                .map(this::toDto)
                .toList();

            return Result.success(dtos);

        } catch (Exception e) {
            log.error("Error getting all {}", getEntityName() + "s", e);
            return Result.failure("Failed to get " + getEntityName() + "s: " + e.getMessage());
        }
    }

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============

    /**
     * {@inheritDoc}
     *
     * Implementation:
     * Uses repository.existsById() for efficient validation without loading entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Result<Void> validateExists(ID id) {
        if (id == null) {
            return Result.failure(getEntityName() + " ID is required");
        }
        if (!repository.existsById(id)) {
            return Result.failure(getEntityName() + " not found");
        }
        return Result.success(null);
    }

    /**
     * {@inheritDoc}
     *
     * Implementation:
     * Simple repository lookup, returns null if not found.
     */
    @Override
    @Transactional(readOnly = true)
    public E getEntityById(ID id) {
        if (id == null) {
            return null;
        }
        return repository.findById(id).orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * Implementation:
     * Uses repository.findAllById() for batch retrieval.
     */
    @Override
    @Transactional(readOnly = true)
    public List<E> getEntitiesByIds(List<ID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids);
    }

    /**
     * {@inheritDoc}
     *
     * Implementation:
     * Delegates to repository.existsById().
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }
        return repository.existsById(id);
    }

    // ============= WRITE OPERATIONS (CUD of CRUD) =============

    /**
     * {@inheritDoc}
     *
     * Implementation:
     * 1. Validates ID and DTO
     * 2. Loads entity
     * 3. Calls abstract updateEntityFromDto() to apply changes
     * 4. Saves entity
     * 5. Returns updated DTO
     */
    @Override
    @Transactional
    public Result<D> update(ID id, D dto) {
        try {
            if (id == null) {
                return Result.failure(getEntityName() + " ID is required");
            }
            if (dto == null) {
                return Result.failure(getEntityName() + " data is required");
            }

            E entity = repository.findById(id).orElse(null);
            if (entity == null) {
                return Result.failure(getEntityName() + " not found");
            }

            // Apply DTO changes to entity (subclass implements the mapping logic)
            updateEntityFromDto(entity, dto);

            E savedEntity = repository.save(entity);

            log.info("{} updated: {}", getEntityName(), id);

            return Result.success(toDto(savedEntity));

        } catch (Exception e) {
            log.error("Error updating {}", getEntityName(), e);
            return Result.failure("Failed to update " + getEntityName() + ": " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * Implementation:
     * Uses repository.deleteById() for hard delete.
     * Override this method for soft deletes or relationship cleanup.
     */
    @Override
    @Transactional
    public Result<Void> deleteById(ID id) {
        try {
            if (id == null) {
                return Result.failure(getEntityName() + " ID is required");
            }

            if (!repository.existsById(id)) {
                return Result.failure(getEntityName() + " not found");
            }

            repository.deleteById(id);

            log.info("{} deleted: {}", getEntityName(), id);

            return Result.success(null);

        } catch (Exception e) {
            log.error("Error deleting {}", getEntityName(), e);
            return Result.failure("Failed to delete " + getEntityName() + ": " + e.getMessage());
        }
    }

    // ============= ABSTRACT METHODS TO BE IMPLEMENTED BY SUBCLASSES =============

    /**
     * Converts an entity to its DTO representation.
     * Subclasses must implement this using their specific mapper.
     *
     * @param entity Entity to convert
     * @return DTO representation of the entity
     */
    protected abstract D toDto(E entity);

    /**
     * Returns the entity name for error messages.
     * Used in logging and error message formatting.
     *
     * @return Entity name (e.g., "Patient", "ConsultationType", "Diagnosis")
     */
    protected abstract String getEntityName();

    /**
     * Updates an entity with data from a DTO.
     * Subclasses must implement this to copy relevant fields from DTO to entity.
     *
     * This method is called by the update() template method.
     * Only update fields that should be modifiable via update operation.
     *
     * Example:
     * <pre>
     * {@code
     * @Override
     * protected void updateEntityFromDto(Patient entity, PatientDto dto) {
     *     if (dto.getPhone() != null) {
     *         entity.setPhone(dto.getPhone());
     *     }
     *     if (dto.getEmail() != null) {
     *         entity.setEmail(dto.getEmail());
     *     }
     *     // Don't update ID, createdDate, or other immutable fields
     * }
     * }
     * </pre>
     *
     * @param entity Entity to update (already loaded from database)
     * @param dto DTO containing new values
     */
    protected abstract void updateEntityFromDto(E entity, D dto);
}
