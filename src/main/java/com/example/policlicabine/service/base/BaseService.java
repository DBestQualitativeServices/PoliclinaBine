package com.example.policlicabine.service.base;

import com.example.policlicabine.common.Result;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Generic base service interface defining common CRUD operations.
 *
 * All services managing entities follow these patterns:
 * - Each service manages exactly ONE entity type
 * - Public API methods return Result&lt;DTO&gt; for consistent error handling
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
 *     public Result<PatientDto> registerNewPatient(...) {
 *         // Custom logic
 *     }
 * }
 * }
 * </pre>
 */
public interface BaseService<E, D, ID> {

    // ============= PUBLIC API METHODS (Return Result<DTO>) =============

    /**
     * Finds an entity by its unique identifier.
     * PUBLIC API method returning Result&lt;DTO&gt; for external use (controllers).
     *
     * @param id Entity identifier
     * @return Result containing DTO or error message
     */
    @Transactional(readOnly = true)
    Result<D> findById(ID id);

    /**
     * Retrieves all entities.
     * PUBLIC API method returning Result&lt;List&lt;DTO&gt;&gt; for external use.
     *
     * Note: Use with caution for large datasets. Consider pagination for production.
     *
     * @return Result containing list of DTOs or error message
     */
    @Transactional(readOnly = true)
    Result<List<D>> findAll();

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============

    /**
     * INTERNAL: Validates that an entity exists.
     * Used by other services for validation with clear error messaging.
     * Returns Result for error message propagation.
     *
     * Example:
     * <pre>
     * {@code
     * // In AppointmentSessionService
     * Result<Void> check = patientService.validateExists(patientId);
     * if (check.isFailure()) {
     *     return Result.failure(check.getErrorMessage());
     * }
     * }
     * </pre>
     *
     * @param id Entity identifier
     * @return Result success if entity exists, failure with message otherwise
     */
    @Transactional(readOnly = true)
    Result<Void> validateExists(ID id);

    /**
     * INTERNAL: Gets an entity by ID.
     * Used by other services for entity access without Result wrapper.
     * Returns entity directly, null if not found.
     *
     * Example:
     * <pre>
     * {@code
     * // In InvoiceService
     * User user = userService.getEntityById(userId);
     * if (user == null) {
     *     return Result.failure("User not found");
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
     * PUBLIC API method returning Result&lt;DTO&gt; for external use (controllers).
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
     * @return Result containing updated DTO or error message
     */
    @Transactional
    Result<D> update(ID id, D dto);

    /**
     * Deletes an entity by its unique identifier.
     * PUBLIC API method returning Result&lt;Void&gt; for external use (controllers).
     *
     * Default implementation performs hard delete using repository.deleteById().
     * Services can override this method for:
     * - Soft deletes (setting isActive = false)
     * - Relationship cleanup (bidirectional mappings)
     * - Publishing domain events
     * - Additional validation
     *
     * @param id Entity identifier
     * @return Result success if deleted, failure with message otherwise
     */
    @Transactional
    Result<Void> deleteById(ID id);
}
