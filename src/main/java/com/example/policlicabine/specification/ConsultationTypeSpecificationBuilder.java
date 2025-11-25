package com.example.policlicabine.specification;

import com.example.policlicabine.common.specification.SpecificationBuilder;
import com.example.policlicabine.dto.ConsultationTypeFilterCriteria;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.enums.Specialty;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class ConsultationTypeSpecificationBuilder implements SpecificationBuilder<ConsultationType, ConsultationTypeFilterCriteria> {

    @Override
    public Specification<ConsultationType> build(ConsultationTypeFilterCriteria criteria) {
        return Specification.where(hasName(criteria.getName()))
                .and(hasSpecialty(criteria.getSpecialty()))
                .and(isActive(criteria.getIsActive()))
                .and(hasPriceGreaterThanOrEqual(criteria.getMinPrice()))
                .and(hasPriceLessThanOrEqual(criteria.getMaxPrice()))
                .and(hasPriceCurrency(criteria.getPriceCurrency()))
                .and(hasDurationGreaterThanOrEqual(criteria.getMinDurationMinutes()))
                .and(hasDurationLessThanOrEqual(criteria.getMaxDurationMinutes()))
                .and(requiresSurgeryRoom(criteria.getRequiresSurgeryRoom()))
                .and(createdAfter(criteria.getCreatedAfter()))
                .and(createdBefore(criteria.getCreatedBefore()));
    }

    private Specification<ConsultationType> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    private Specification<ConsultationType> hasSpecialty(Specialty specialty) {
        return (root, query, cb) -> {
            if (specialty == null) {
                return null;
            }
            return cb.equal(root.get("specialty"), specialty);
        };
    }

    private Specification<ConsultationType> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return null;
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    private Specification<ConsultationType> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    private Specification<ConsultationType> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    private Specification<ConsultationType> hasPriceCurrency(String priceCurrency) {
        return (root, query, cb) -> {
            if (priceCurrency == null || priceCurrency.trim().isEmpty()) {
                return null;
            }
            return cb.equal(root.get("priceCurrency"), priceCurrency);
        };
    }

    private Specification<ConsultationType> hasDurationGreaterThanOrEqual(Integer minDurationMinutes) {
        return (root, query, cb) -> {
            if (minDurationMinutes == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("durationMinutes"), minDurationMinutes);
        };
    }

    private Specification<ConsultationType> hasDurationLessThanOrEqual(Integer maxDurationMinutes) {
        return (root, query, cb) -> {
            if (maxDurationMinutes == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("durationMinutes"), maxDurationMinutes);
        };
    }

    private Specification<ConsultationType> requiresSurgeryRoom(Boolean requiresSurgeryRoom) {
        return (root, query, cb) -> {
            if (requiresSurgeryRoom == null) {
                return null;
            }
            return cb.equal(root.get("requiresSurgeryRoom"), requiresSurgeryRoom);
        };
    }

    private Specification<ConsultationType> createdAfter(OffsetDateTime createdAfter) {
        return (root, query, cb) -> {
            if (createdAfter == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
        };
    }

    private Specification<ConsultationType> createdBefore(OffsetDateTime createdBefore) {
        return (root, query, cb) -> {
            if (createdBefore == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore);
        };
    }
}
