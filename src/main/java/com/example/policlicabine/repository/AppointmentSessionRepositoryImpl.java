package com.example.policlicabine.repository;

/**
 * ====================================================================================================
 * CUSTOM REPOSITORY IMPLEMENTATION - CURRENTLY DISABLED
 * ====================================================================================================
 *
 * This is the implementation of AppointmentSessionRepositoryCustom interface.
 * See AppointmentSessionRepositoryCustom.java for full documentation on:
 * - Why this was created
 * - How it works (two-phase query pattern)
 * - When to reactivate
 * - Performance benefits
 *
 * TO REACTIVATE:
 * 1. Uncomment the entire class implementation below (starting at line 70)
 * 2. Follow instructions in AppointmentSessionRepositoryCustom.java header
 *
 * ====================================================================================================
 */

/*
 * IMPLEMENTATION CODE - Uncomment entire section when reactivating
 *
import com.example.policlicabine.entity.AppointmentSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class AppointmentSessionRepositoryImpl implements AppointmentSessionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<UUID> findSessionIds(Specification<AppointmentSession> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query for total elements
        long total = executeCountQuery(cb, spec);

        if (total == 0) {
            return Page.empty(pageable);
        }

        // ID query with pagination
        List<UUID> ids = executeIdQuery(cb, spec, pageable);

        return new PageImpl<>(ids, pageable, total);
    }

    private long executeCountQuery(CriteriaBuilder cb, Specification<AppointmentSession> spec) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AppointmentSession> countRoot = countQuery.from(AppointmentSession.class);

        countQuery.select(cb.count(countRoot));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(countRoot, countQuery, cb);
            if (predicate != null) {
                countQuery.where(predicate);
            }
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<UUID> executeIdQuery(CriteriaBuilder cb, Specification<AppointmentSession> spec, Pageable pageable) {
        CriteriaQuery<UUID> idQuery = cb.createQuery(UUID.class);
        Root<AppointmentSession> root = idQuery.from(AppointmentSession.class);

        idQuery.select(root.get("sessionId"));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, idQuery, cb);
            if (predicate != null) {
                idQuery.where(predicate);
            }
        }

        if (pageable.getSort().isSorted()) {
            idQuery.orderBy(pageable.getSort().stream()
                    .map(order -> order.isAscending()
                            ? cb.asc(root.get(order.getProperty()))
                            : cb.desc(root.get(order.getProperty())))
                    .toList());
        }

        TypedQuery<UUID> typedQuery = entityManager.createQuery(idQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return typedQuery.getResultList();
    }
}
*/

// Placeholder class to prevent compilation errors
// Remove this class when uncommenting the implementation above
public class AppointmentSessionRepositoryImpl {
    // This empty class exists only to prevent build errors
    // The real implementation is commented out above
    // See AppointmentSessionRepositoryCustom.java for reactivation instructions
}
