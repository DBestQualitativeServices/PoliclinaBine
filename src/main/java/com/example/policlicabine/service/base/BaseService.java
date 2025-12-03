package com.example.policlicabine.service.base;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Generic base service interface defining common CRUD operations.
 *
 * All services managing entities follow these patterns:
 * - Each service manages exactly ONE entity type
 * - Public API methods return DTOs directly, throwing exceptions on errors
 * - Internal methods return entities directly for service-to-service communication
 * - Transaction boundaries: readOnly for queries, default for writes
 *
 * Type Parameters:
 * @param <E> Entity type (e.g., Patient, ConsultationType, Diagnosis)
 * @param <D> DTO type (e.g., PatientDto, ConsultationDto, DiagnosisDto)
 * @param <ID> ID type (typically UUID)
 *
 * Architecture Benefits:
 * - Eliminates duplicate CRUD implementations (~30-40% code reduction)
 * - Consistent API across all services
 * - Type-safe with generics
 * - Optional adoption (services can extend or not)
 *
 * Example Usage:
 * <pre>
 * {@code
 * @Service
 * public class PatientService extends BaseServiceImpl<Patient, PatientDto, UUID> {
 *
 *     public PatientService(PatientRepository repository, PatientMapper mapper) {
 *         super(repository, mapper);
 *     }
 *
 *     // Business-specific methods
 *     public PatientDto registerNewPatient(...) {
 *         // Custom logic
 *     }
 * }
 * }
 * </pre>
 */
public interface BaseService<E, D, ID> {

    // ============= PUBLIC API METHODS (Return DTO, throw exceptions) =============

    /**
     * Finds an entity by its unique identifier.
     * PUBLIC API method returning DTO for external use (controllers).
     *
     * @param id Entity identifier
     * @return DTO representation of the entity
     * @throws com.example.policlicabine.exception.ResourceNotFoundException if entity not found
     * @throws com.example.policlicabine.exception.BusinessException if ID is null
     */
    @Transactional(readOnly = true)
    D findById(ID id);

    /**
     * Retrieves all entities.
     * PUBLIC API method returning List of DTOs for external use.
     *
     * Note: Use with caution for large datasets. Consider pagination for production.
     *
     * @return List of DTOs (never null, may be empty)
     */
    @Transactional(readOnly = true)
    List<D> findAll();

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============

    /**
     * INTERNAL: Validates that an entity exists.
     * Used by other services for validation.
     * Throws exception if entity does not exist.
     *
     * Example:
     * <pre>
     * {@code
     * // In AppointmentSessionService
     * patientService.validateExists(patientId); // throws if not found
     * }
     * </pre>
     *
     * @param id Entity identifier
     * @throws com.example.policlicabine.exception.ResourceNotFoundException if entity not found
     * @throws com.example.policlicabine.exception.BusinessException if ID is null
     */
    @Transactional(readOnly = true)
    void validateExists(ID id);

    /**
     * INTERNAL: Gets an entity by ID.
     * Used by other services for entity access.
     * Returns entity directly, null if not found.
     *
     * Example:
     * <pre>
     * {@code
     * // In InvoiceService
     * User user = userService.getEntityById(userId);
     * if (user == null) {
     *     throw new ResourceNotFoundException("User", userId);
     * }
     * }
     * </pre>
     *
     * @param id Entity identifier
     * @return Entity or null if not found
     */
    @Transactional(readOnly = true)
    E getEntityById(ID id);

    /**
     * INTERNAL: Gets multiple entities by their IDs.
     * Used by other services for batch entity access.
     * Returns list of entities (may be empty, never null).
     *
     * Example:
     * <pre>
     * {@code
     * // In AppointmentSessionService
     * List<Diagnosis> diagnoses = diagnosisService.getEntitiesByIds(diagnosisIds);
     * }
     * </pre>
     *
     * @param ids List of entity identifiers
     * @return List of entities (may be empty, never null)
     */
    @Transactional(readOnly = true)
    List<E> getEntitiesByIds(List<ID> ids);

    /**
     * INTERNAL: Checks if an entity exists by ID.
     * Used internally for validation without loading the full entity.
     * More performant than getEntityById() for existence checks.
     *
     * @param id Entity identifier
     * @return true if entity exists, false otherwise
     */
    @Transactional(readOnly = true)
    boolean existsById(ID id);

    // ============= WRITE OPERATIONS (CUD of CRUD) =============

    /**
     * Updates an entity with data from a DTO.
     * PUBLIC API method returning DTO for external use (controllers).
     *
     * Implementation uses template method pattern:
     * - Loads entity by ID
     * - Calls abstract updateEntityFromDto() to apply changes
     * - Saves and returns updated entity as DTO
     *
     * Services can override this method for custom logic (validation, events, etc.)
     *
     * @param id Entity identifier
     * @param dto DTO containing updated data
     * @return Updated DTO
     * @throws com.example.policlicabine.exception.ResourceNotFoundException if entity not found
     * @throws com.example.policlicabine.exception.BusinessException if validation fails
     */
    @Transactional
    D update(ID id, D dto);

    /**
     * Deletes an entity by its unique identifier.
     * PUBLIC API method for external use (controllers).
     *
     * Default implementation performs hard delete using repository.deleteById().
     * Services can override this method for:
     * - Soft deletes (setting isActive = false)
     * - Relationship cleanup (bidirectional mappings)
     * - Publishing domain events
     * - Additional validation
     *
     * @param id Entity identifier
     * @throws com.example.policlicabine.exception.ResourceNotFoundException if entity not found
     * @throws com.example.policlicabine.exception.BusinessException if ID is null
     */
    @Transactional
    void deleteById(ID id);
}
