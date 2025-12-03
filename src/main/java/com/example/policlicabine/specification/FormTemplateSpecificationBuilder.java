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

        return Specification.where(hasName(criteria.getName()))
                .and(isActive(criteria.getActive()))
                .and(isDeleted(criteria.getIsDeleted()))
                .and(createdAfter(criteria.getCreatedAfter()))
                .and(createdBefore(criteria.getCreatedBefore()))
                .and(createdByUser(criteria.getCreatedByUserId()));
    }

    private Specification<FormTemplate> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%");
        };
    }

    private Specification<FormTemplate> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }

    private Specification<FormTemplate> isDeleted(Boolean isDeleted) {
        return (root, query, cb) -> isDeleted == null ? null : cb.equal(root.get("isDeleted"), isDeleted);
    }

    private Specification<FormTemplate> createdAfter(LocalDateTime createdAfter) {
        return (root, query, cb) -> createdAfter == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
    }

    private Specification<FormTemplate> createdBefore(LocalDateTime createdBefore) {
        return (root, query, cb) -> createdBefore == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore);
    }

    private Specification<FormTemplate> createdByUser(UUID createdByUserId) {
        return (root, query, cb) -> createdByUserId == null ? null : cb.equal(root.get("createdBy").get("userId"), createdByUserId);
    }
}
