package com.example.policlicabine.common.specification;

import org.springframework.data.jpa.domain.Specification;

/**
 * Generic specification builder interface for converting filter criteria to JPA Specifications.
 * <p>
 * This functional interface provides a standard way to build dynamic database queries
 * from filter criteria DTOs across all entities in the application.
 * </p>
 *
 * @param <E> the entity type
 * @param <F> the filter criteria type
 * @since 1.0
 */
@FunctionalInterface
public interface SpecificationBuilder<E, F> {

    /**
     * Builds a JPA Specification from the given filter criteria.
     * <p>
     * Implementations should combine multiple filter conditions using
     * {@code Specification.where().and()} pattern, handling null values
     * gracefully by returning null predicates for unset criteria.
     * </p>
     *
     * @param filterCriteria the filter criteria containing search parameters
     * @return a JPA Specification combining all non-null filter criteria
     */
    Specification<E> build(F filterCriteria);
}
