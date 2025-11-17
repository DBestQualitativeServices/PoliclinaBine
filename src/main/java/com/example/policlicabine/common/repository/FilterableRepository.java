package com.example.policlicabine.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository interface combining JPA repository functionality with Specification support.
 * <p>
 * This interface provides both standard CRUD operations (from {@link JpaRepository})
 * and dynamic query capabilities (from {@link JpaSpecificationExecutor}), enabling
 * repositories to support filtering, pagination, and sorting with Specifications.
 * </p>
 * <p>
 * All domain repositories should extend this interface instead of directly extending
 * {@code JpaRepository} to inherit filtering capabilities.
 * </p>
 *
 * @param <E> the entity type
 * @param <ID> the type of the entity identifier
 * @since 1.0
 */
@NoRepositoryBean
public interface FilterableRepository<E, ID> extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {

    // This interface intentionally has no methods.
    // It combines the following inherited methods from JpaSpecificationExecutor:
    //
    // - Optional<E> findOne(Specification<E> spec)
    // - List<E> findAll(Specification<E> spec)
    // - Page<E> findAll(Specification<E> spec, Pageable pageable)
    // - List<E> findAll(Specification<E> spec, Sort sort)
    // - long count(Specification<E> spec)
    // - boolean exists(Specification<E> spec)
    // - long delete(Specification<E> spec)
}
