package com.example.policlicabine.specification;

import com.example.policlicabine.common.specification.SpecificationBuilder;
import com.example.policlicabine.dto.FormTemplateFilterCriteria;
import com.example.policlicabine.entity.FormTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builds JPA Specifications for FormTemplate entity from filter criteria.
 *
 * Converts FormTemplateFilterCriteria to dynamic JPA queries.
 * All filters are optional and combined with AND logic.
 * Null filters are skipped (not included in the WHERE clause).
 */
@Component
@Slf4j
public class FormTemplateSpecificationBuilder
    implements SpecificationBuilder<FormTemplate, FormTemplateFilterCriteria> {

    @Override
    public Specification<FormTemplate> build(FormTemplateFilterCriteria criteria) {
        log.debug("Building FormTemplate specification from criteria: {}", criteria);

        return Specification.where(hasCode(criteria.getCode()))
                .and(hasName(criteria.getName()))
                .and(hasPurpose(criteria.getPurpose()))
                .and(isActive(criteria.getActive()))
                .and(isDeleted(criteria.getIsDeleted()))
                .and(createdAfter(criteria.getCreatedAfter()))
                .and(createdBefore(criteria.getCreatedBefore()))
                .and(createdByUser(criteria.getCreatedByUserId()));
    }

    /**
     * Filter by template code (partial match).
     * Case-insensitive LIKE search with wildcards.
     */
    private Specification<FormTemplate> hasCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.trim().isEmpty()) {
                return null;  // Skip this filter if not provided
            }
            return cb.like(
                    cb.lower(root.get("code")),
                    "%" + code.trim().toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter by template name (partial match).
     * Case-insensitive LIKE search with wildcards.
     */
    private Specification<FormTemplate> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return null;  // Skip this filter if not provided
            }
            return cb.like(
                    cb.lower(root.get("name")),
                    "%" + name.trim().toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter by FormPurpose (exact match).
     */
    private Specification<FormTemplate> hasPurpose(Object purpose) {
        return (root, query, cb) -> {
            if (purpose == null) {
                return null;  // Skip this filter if not provided
            }
            return cb.equal(root.get("purpose"), purpose);
        };
    }

    /**
     * Filter by active status (boolean).
     */
    private Specification<FormTemplate> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return null;  // Skip this filter if not provided
            }
            return cb.equal(root.get("active"), active);
        };
    }

    /**
     * Filter by deletion status (boolean).
     * When true, returns only deleted templates.
     * When false, returns only non-deleted templates.
     */
    private Specification<FormTemplate> isDeleted(Boolean isDeleted) {
        return (root, query, cb) -> {
            if (isDeleted == null) {
                return null;  // Skip this filter if not provided (return all regardless of deletion status)
            }
            return cb.equal(root.get("isDeleted"), isDeleted);
        };
    }

    /**
     * Filter by creation date (inclusive, >= createdAfter).
     */
    private Specification<FormTemplate> createdAfter(LocalDateTime createdAfter) {
        return (root, query, cb) -> {
            if (createdAfter == null) {
                return null;  // Skip this filter if not provided
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
        };
    }

    /**
     * Filter by creation date (inclusive, <= createdBefore).
     */
    private Specification<FormTemplate> createdBefore(LocalDateTime createdBefore) {
        return (root, query, cb) -> {
            if (createdBefore == null) {
                return null;  // Skip this filter if not provided
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore);
        };
    }

    /**
     * Filter by creator user ID.
     */
    private Specification<FormTemplate> createdByUser(UUID createdByUserId) {
        return (root, query, cb) -> {
            if (createdByUserId == null) {
                return null;  // Skip this filter if not provided
            }
            return cb.equal(root.get("createdBy").get("userId"), createdByUserId);
        };
    }
}
