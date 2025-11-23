package com.example.policlicabine.specification;

import com.example.policlicabine.common.specification.SpecificationBuilder;
import com.example.policlicabine.dto.DoctorFilterCriteria;
import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.enums.Specialty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Slf4j
public class DoctorSpecificationBuilder implements SpecificationBuilder<Doctor, DoctorFilterCriteria> {

    @Override
    public Specification<Doctor> build(DoctorFilterCriteria criteria) {
        log.debug("Building doctor specification from criteria: {}", criteria);

        return Specification.where(hasFullName(criteria.getFullName()))
                .and(hasSpecialty(criteria.getSpecialty()))
                .and(isEnabled(criteria.getEnabled()))
                .and(createdAfter(criteria.getCreatedAfter()))
                .and(createdBefore(criteria.getCreatedBefore()));
    }

    private Specification<Doctor> hasFullName(String fullName) {
        return (root, query, cb) -> {
            if (fullName == null || fullName.trim().isEmpty()) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("user").get("fullName")),
                    "%" + fullName.trim().toLowerCase() + "%"
            );
        };
    }

    private Specification<Doctor> hasSpecialty(Specialty specialty) {
        return (root, query, cb) -> {
            if (specialty == null) {
                return null;
            }
            return cb.isMember(specialty, root.get("specialties"));
        };
    }

    private Specification<Doctor> isEnabled(Boolean enabled) {
        return (root, query, cb) -> {
            if (enabled == null) {
                return null;
            }
            return cb.equal(root.get("user").get("enabled"), enabled);
        };
    }

    private Specification<Doctor> createdAfter(OffsetDateTime createdAfter) {
        return (root, query, cb) -> {
            if (createdAfter == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("user").get("createdAt"), createdAfter);
        };
    }

    private Specification<Doctor> createdBefore(OffsetDateTime createdBefore) {
        return (root, query, cb) -> {
            if (createdBefore == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("user").get("createdAt"), createdBefore);
        };
    }
}
