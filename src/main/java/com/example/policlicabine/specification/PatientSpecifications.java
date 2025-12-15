package com.example.policlicabine.specification;

import com.example.policlicabine.entity.Patient;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Static factory methods for creating JPA Specifications for Patient entity.
 * <p>
 * This utility class provides reusable specification predicates that can be combined
 * using {@code Specification.where().and().or()} for dynamic queries.
 * </p>
 * <p>
 * This is an alternative to {@link PatientSpecificationBuilder} and can be used
 * directly in services when more control over specification composition is needed.
 * </p>
 *
 * @since 1.0
 */
public final class PatientSpecifications {

    private PatientSpecifications() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Filter by first name (case-insensitive partial match).
     *
     * @param firstName the first name to search for
     * @return specification or null if firstName is null/blank
     */
    public static Specification<Patient> hasFirstName(String firstName) {
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
     * @param lastName the last name to search for
     * @return specification or null if lastName is null/blank
     */
    public static Specification<Patient> hasLastName(String lastName) {
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
     * @param phone the phone number to search for
     * @return specification or null if phone is null/blank
     */
    public static Specification<Patient> hasPhone(String phone) {
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
     * @param email the email to search for
     * @return specification or null if email is null/blank
     */
    public static Specification<Patient> hasEmail(String email) {
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
     *
     * @param fullName the name to search for
     * @return specification or null if fullName is null/blank
     */
    public static Specification<Patient> hasFullName(String fullName) {
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
     * @param registeredAfter the minimum registration date (inclusive)
     * @return specification or null if registeredAfter is null
     */
    public static Specification<Patient> registeredAfter(OffsetDateTime registeredAfter) {
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
     * @param registeredBefore the maximum registration date (inclusive)
     * @return specification or null if registeredBefore is null
     */
    public static Specification<Patient> registeredBefore(OffsetDateTime registeredBefore) {
        return (root, query, cb) -> {
            if (registeredBefore == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("registrationDate"), registeredBefore);
        };
    }
}
