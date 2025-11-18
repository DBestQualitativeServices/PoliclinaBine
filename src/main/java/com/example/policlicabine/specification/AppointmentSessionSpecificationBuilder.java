package com.example.policlicabine.specification;

import com.example.policlicabine.common.specification.SpecificationBuilder;
import com.example.policlicabine.dto.AppointmentSessionFilterCriteria;
import com.example.policlicabine.entity.AppointmentSession;
import com.example.policlicabine.entity.Consultation;
import com.example.policlicabine.entity.Doctor;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.SessionStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Specification builder for AppointmentSession entity filtering.
 * <p>
 * Converts {@link AppointmentSessionFilterCriteria} into JPA {@link Specification}
 * for dynamic database queries with multiple filter conditions.
 * </p>
 * <p>
 * All filters are combined with AND logic. Null filter values are ignored.
 * Text filters use case-insensitive partial matching (LIKE %value%).
 * Supports joins to Patient, Doctor, User, and Consultation entities.
 * </p>
 *
 * @since 1.0
 */
@Component
@Slf4j
public class AppointmentSessionSpecificationBuilder implements SpecificationBuilder<AppointmentSession, AppointmentSessionFilterCriteria> {

    @Override
    public Specification<AppointmentSession> build(AppointmentSessionFilterCriteria criteria) {
        log.debug("Building appointment session specification from criteria: {}", criteria);

        return Specification.where(hasPatientId(criteria.getPatientId()))
                .and(hasPatientNameLike(criteria.getPatientName()))
                .and(hasDoctorId(criteria.getDoctorId()))
                .and(hasDoctorNameLike(criteria.getDoctorName()))
                .and(scheduledAfter(criteria.getScheduledAfter()))
                .and(scheduledBefore(criteria.getScheduledBefore()))
                .and(completedAfter(criteria.getCompletedAfter()))
                .and(completedBefore(criteria.getCompletedBefore()))
                .and(hasStatus(criteria.getStatus()))
                .and(hasConsultationTypes(criteria.getConsultationNames()));
    }

    /**
     * Filter by exact patient ID.
     *
     * @param patientId the patient ID to filter by (can be null)
     * @return specification or null if patientId is null
     */
    private Specification<AppointmentSession> hasPatientId(UUID patientId) {
        return (root, query, cb) -> {
            if (patientId == null) {
                return null;
            }
            return cb.equal(root.get("patient").get("patientId"), patientId);
        };
    }

    /**
     * Filter by patient name (case-insensitive partial match on first name OR last name).
     *
     * @param patientName the patient name to search for (can be null)
     * @return specification or null if patientName is null/blank
     */
    private Specification<AppointmentSession> hasPatientNameLike(String patientName) {
        return (root, query, cb) -> {
            if (patientName == null || patientName.trim().isEmpty()) {
                return null;
            }
            Join<AppointmentSession, Patient> patientJoin = root.join("patient");
            String searchPattern = "%" + patientName.trim().toLowerCase() + "%";

            Predicate firstNameMatch = cb.like(
                    cb.lower(patientJoin.get("firstName")),
                    searchPattern
            );
            Predicate lastNameMatch = cb.like(
                    cb.lower(patientJoin.get("lastName")),
                    searchPattern
            );

            return cb.or(firstNameMatch, lastNameMatch);
        };
    }

    /**
     * Filter by exact doctor ID.
     *
     * @param doctorId the doctor ID to filter by (can be null)
     * @return specification or null if doctorId is null
     */
    private Specification<AppointmentSession> hasDoctorId(UUID doctorId) {
        return (root, query, cb) -> {
            if (doctorId == null) {
                return null;
            }
            return cb.equal(root.get("doctor").get("doctorId"), doctorId);
        };
    }

    /**
     * Filter by doctor name (case-insensitive partial match on doctor's user full name).
     *
     * @param doctorName the doctor name to search for (can be null)
     * @return specification or null if doctorName is null/blank
     */
    private Specification<AppointmentSession> hasDoctorNameLike(String doctorName) {
        return (root, query, cb) -> {
            if (doctorName == null || doctorName.trim().isEmpty()) {
                return null;
            }
            Join<AppointmentSession, Doctor> doctorJoin = root.join("doctor");
            Join<Doctor, User> userJoin = doctorJoin.join("user");

            return cb.like(
                    cb.lower(userJoin.get("fullName")),
                    "%" + doctorName.trim().toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter sessions scheduled on or after the specified date/time.
     *
     * @param scheduledAfter the minimum scheduled date (inclusive, can be null)
     * @return specification or null if scheduledAfter is null
     */
    private Specification<AppointmentSession> scheduledAfter(OffsetDateTime scheduledAfter) {
        return (root, query, cb) -> {
            if (scheduledAfter == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("scheduledDateTime"), scheduledAfter);
        };
    }

    /**
     * Filter sessions scheduled on or before the specified date/time.
     *
     * @param scheduledBefore the maximum scheduled date (inclusive, can be null)
     * @return specification or null if scheduledBefore is null
     */
    private Specification<AppointmentSession> scheduledBefore(OffsetDateTime scheduledBefore) {
        return (root, query, cb) -> {
            if (scheduledBefore == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("scheduledDateTime"), scheduledBefore);
        };
    }

    /**
     * Filter sessions completed on or after the specified date/time.
     *
     * @param completedAfter the minimum completion date (inclusive, can be null)
     * @return specification or null if completedAfter is null
     */
    private Specification<AppointmentSession> completedAfter(OffsetDateTime completedAfter) {
        return (root, query, cb) -> {
            if (completedAfter == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("actualCompletionTime"), completedAfter);
        };
    }

    /**
     * Filter sessions completed on or before the specified date/time.
     *
     * @param completedBefore the maximum completion date (inclusive, can be null)
     * @return specification or null if completedBefore is null
     */
    private Specification<AppointmentSession> completedBefore(OffsetDateTime completedBefore) {
        return (root, query, cb) -> {
            if (completedBefore == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("actualCompletionTime"), completedBefore);
        };
    }

    /**
     * Filter by session status (exact match).
     *
     * @param status the status to filter by (can be null)
     * @return specification or null if status is null
     */
    private Specification<AppointmentSession> hasStatus(SessionStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    /**
     * Filter sessions containing ANY of the specified consultation types.
     * <p>
     * Sessions that have at least one consultation matching any name in the list will be included.
     * </p>
     *
     * @param consultationNames the list of consultation names to search for (can be null)
     * @return specification or null if consultationNames is null/empty
     */
    private Specification<AppointmentSession> hasConsultationTypes(List<String> consultationNames) {
        return (root, query, cb) -> {
            if (consultationNames == null || consultationNames.isEmpty()) {
                return null;
            }
            Join<AppointmentSession, Consultation> consultationJoin = root.join("consultations");
            return consultationJoin.get("name").in(consultationNames);
        };
    }
}
