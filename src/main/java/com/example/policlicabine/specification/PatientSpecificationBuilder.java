package com.example.policlicabine.specification;

import com.example.policlicabine.common.specification.SpecificationBuilder;
import com.example.policlicabine.dto.PatientFilterCriteria;
import com.example.policlicabine.entity.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Specification builder for Patient entity filtering.
 * <p>
 * Converts {@link PatientFilterCriteria} into JPA {@link Specification}
 * for dynamic database queries with multiple filter conditions.
 * </p>
 * <p>
 * All filters are combined with AND logic. Null filter values are ignored.
 * Text filters use case-insensitive partial matching (LIKE %value%).
 * </p>
 *
 * @since 1.0
 */
@Component
@Slf4j
public class PatientSpecificationBuilder implements SpecificationBuilder<Patient, PatientFilterCriteria> {

    @Override
    public Specification<Patient> build(PatientFilterCriteria criteria) {
        log.debug("Building patient specification from criteria: {}", criteria);

        return Specification.where(hasFirstName(criteria.getFirstName()))
                .and(hasLastName(criteria.getLastName()))
                .and(hasFullName(criteria.getFullName()))
                .and(hasPhone(criteria.getPhone()))
                .and(hasEmail(criteria.getEmail()))
                .and(registeredAfter(criteria.getRegisteredAfter()))
                .and(registeredBefore(criteria.getRegisteredBefore()));
    }

    /**
     * Filter by first name (case-insensitive partial match).
     *
     * @param firstName the first name to search for (can be null)
     * @return specification or null if firstName is null/blank
     */
    private Specification<Patient> hasFirstName(String firstName) {
        return (root, query, cb) -> {
            if (firstName == null || firstName.trim().isEmpty()) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("firstName")),
                    "%" + firstName.trim().toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter by last name (case-insensitive partial match).
     *
     * @param lastName the last name to search for (can be null)
     * @return specification or null if lastName is null/blank
     */
    private Specification<Patient> hasLastName(String lastName) {
        return (root, query, cb) -> {
            if (lastName == null || lastName.trim().isEmpty()) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("lastName")),
                    "%" + lastName.trim().toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter by phone number (partial match).
     *
     * @param phone the phone number to search for (can be null)
     * @return specification or null if phone is null/blank
     */
    private Specification<Patient> hasPhone(String phone) {
        return (root, query, cb) -> {
            if (phone == null || phone.trim().isEmpty()) {
                return null;
            }
            return cb.like(
                    root.get("phone"),
                    "%" + phone.trim() + "%"
            );
        };
    }

    /**
     * Filter by email address (case-insensitive partial match).
     *
     * @param email the email to search for (can be null)
     * @return specification or null if email is null/blank
     */
    private Specification<Patient> hasEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.trim().isEmpty()) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("email")),
                    "%" + email.trim().toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter by full name - searches in BOTH firstName OR lastName (case-insensitive partial match).
     * Uses OR logic: matches if EITHER firstName OR lastName contains the search term.
     *
     * @param fullName the name to search for in both firstName and lastName
     * @return specification or null if fullName is null/blank
     */
    private Specification<Patient> hasFullName(String fullName) {
        return (root, query, cb) -> {
            if (fullName == null || fullName.trim().isEmpty()) {
                return null;
            }
            String searchPattern = "%" + fullName.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), searchPattern),
                    cb.like(cb.lower(root.get("lastName")), searchPattern)
            );
        };
    }

    /**
     * Filter patients registered on or after the specified date/time.
     *
     * @param registeredAfter the minimum registration date (inclusive, can be null)
     * @return specification or null if registeredAfter is null
     */
    private Specification<Patient> registeredAfter(java.time.OffsetDateTime registeredAfter) {
        return (root, query, cb) -> {
            if (registeredAfter == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("registrationDate"), registeredAfter);
        };
    }

    /**
     * Filter patients registered on or before the specified date/time.
     *
     * @param registeredBefore the maximum registration date (inclusive, can be null)
     * @return specification or null if registeredBefore is null
     */
    private Specification<Patient> registeredBefore(java.time.OffsetDateTime registeredBefore) {
        return (root, query, cb) -> {
            if (registeredBefore == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("registrationDate"), registeredBefore);
        };
    }
}
