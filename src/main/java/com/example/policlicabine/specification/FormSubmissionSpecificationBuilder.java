package com.example.policlicabine.specification;

import com.example.policlicabine.common.specification.SpecificationBuilder;
import com.example.policlicabine.dto.FormSubmissionFilterCriteria;
import com.example.policlicabine.entity.FormSubmission;
import com.example.policlicabine.entity.FormTemplate;
import com.example.policlicabine.entity.Patient;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Specification builder for FormSubmission entity filtering.
 * <p>
 * Converts {@link FormSubmissionFilterCriteria} into JPA {@link Specification}
 * for dynamic database queries with multiple filter conditions.
 * </p>
 * <p>
 * All filters are combined with AND logic. Null filter values are ignored.
 * Text filters use case-insensitive partial matching (LIKE %value%).
 * Supports joins to Patient and FormTemplate entities.
 * </p>
 *
 * @since 1.0
 */
@Component
@Slf4j
public class FormSubmissionSpecificationBuilder implements SpecificationBuilder<FormSubmission, FormSubmissionFilterCriteria> {

    @Override
    public Specification<FormSubmission> build(FormSubmissionFilterCriteria criteria) {
        log.debug("Building form submission specification from criteria: {}", criteria);

        return Specification.where(hasId(criteria.getId()))
                .and(hasPatientId(criteria.getPatientId()))
                .and(hasPatientNameLike(criteria.getPatientName()))
                .and(hasTemplateId(criteria.getTemplateId()))
                .and(hasTemplateNameLike(criteria.getTemplateName()))
                .and(hasAppointmentSessionId(criteria.getAppointmentSessionId()))
                .and(hasConsultationTypeId(criteria.getConsultationTypeId()))
                .and(submittedAfter(criteria.getSubmittedAfter()))
                .and(submittedBefore(criteria.getSubmittedBefore()))
                .and(expiresAfter(criteria.getExpiresAfter()))
                .and(expiresBefore(criteria.getExpiresBefore()))
                .and(isSigned(criteria.getIsSigned()))
                .and(isExpired(criteria.getIsExpired()))
                .and(hasSignedByUser(criteria.getSignedByUserId()));
    }

    /**
     * Filter by exact submission ID.
     *
     * @param id the submission ID to filter by (can be null)
     * @return specification or null if id is null
     */
    private Specification<FormSubmission> hasId(UUID id) {
        return (root, query, cb) -> {
            if (id == null) {
                return null;
            }
            return cb.equal(root.get("id"), id);
        };
    }

    /**
     * Filter by exact patient ID.
     *
     * @param patientId the patient ID to filter by (can be null)
     * @return specification or null if patientId is null
     */
    private Specification<FormSubmission> hasPatientId(UUID patientId) {
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
    private Specification<FormSubmission> hasPatientNameLike(String patientName) {
        return (root, query, cb) -> {
            if (patientName == null || patientName.trim().isEmpty()) {
                return null;
            }
            Join<FormSubmission, Patient> patientJoin = root.join("patient");
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
     * Filter by exact template ID.
     *
     * @param templateId the template ID to filter by (can be null)
     * @return specification or null if templateId is null
     */
    private Specification<FormSubmission> hasTemplateId(UUID templateId) {
        return (root, query, cb) -> {
            if (templateId == null) {
                return null;
            }
            return cb.equal(root.get("template").get("id"), templateId);
        };
    }

    /**
     * Filter by template name (case-insensitive partial match).
     *
     * @param templateName the template name to search for (can be null)
     * @return specification or null if templateName is null/blank
     */
    private Specification<FormSubmission> hasTemplateNameLike(String templateName) {
        return (root, query, cb) -> {
            if (templateName == null || templateName.trim().isEmpty()) {
                return null;
            }
            Join<FormSubmission, FormTemplate> templateJoin = root.join("template");

            return cb.like(
                    cb.lower(templateJoin.get("name")),
                    "%" + templateName.trim().toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter by exact appointment session ID.
     *
     * @param appointmentSessionId the appointment session ID to filter by (can be null)
     * @return specification or null if appointmentSessionId is null
     */
    private Specification<FormSubmission> hasAppointmentSessionId(UUID appointmentSessionId) {
        return (root, query, cb) -> {
            if (appointmentSessionId == null) {
                return null;
            }
            return cb.equal(root.get("appointmentSession").get("sessionId"), appointmentSessionId);
        };
    }

    /**
     * Filter by exact consultation type ID.
     *
     * @param consultationTypeId the consultation type ID to filter by (can be null)
     * @return specification or null if consultationTypeId is null
     */
    private Specification<FormSubmission> hasConsultationTypeId(UUID consultationTypeId) {
        return (root, query, cb) -> {
            if (consultationTypeId == null) {
                return null;
            }
            return cb.equal(root.get("consultationType").get("id"), consultationTypeId);
        };
    }

    /**
     * Filter submissions submitted on or after the specified date/time.
     *
     * @param submittedAfter the minimum submission date (inclusive, can be null)
     * @return specification or null if submittedAfter is null
     */
    private Specification<FormSubmission> submittedAfter(LocalDateTime submittedAfter) {
        return (root, query, cb) -> {
            if (submittedAfter == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("submittedAt"), submittedAfter);
        };
    }

    /**
     * Filter submissions submitted on or before the specified date/time.
     *
     * @param submittedBefore the maximum submission date (inclusive, can be null)
     * @return specification or null if submittedBefore is null
     */
    private Specification<FormSubmission> submittedBefore(LocalDateTime submittedBefore) {
        return (root, query, cb) -> {
            if (submittedBefore == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("submittedAt"), submittedBefore);
        };
    }

    /**
     * Filter submissions expiring on or after the specified date/time.
     *
     * @param expiresAfter the minimum expiration date (inclusive, can be null)
     * @return specification or null if expiresAfter is null
     */
    private Specification<FormSubmission> expiresAfter(LocalDateTime expiresAfter) {
        return (root, query, cb) -> {
            if (expiresAfter == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("expiresAt"), expiresAfter);
        };
    }

    /**
     * Filter submissions expiring on or before the specified date/time.
     *
     * @param expiresBefore the maximum expiration date (inclusive, can be null)
     * @return specification or null if expiresBefore is null
     */
    private Specification<FormSubmission> expiresBefore(LocalDateTime expiresBefore) {
        return (root, query, cb) -> {
            if (expiresBefore == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("expiresAt"), expiresBefore);
        };
    }

    /**
     * Filter by signature status.
     * <p>
     * When isSigned is true, returns only submissions where patientSignedAt IS NOT NULL.
     * When isSigned is false, returns only submissions where patientSignedAt IS NULL.
     * </p>
     *
     * @param isSigned the signature filter (can be null)
     * @return specification or null if isSigned is null
     */
    private Specification<FormSubmission> isSigned(Boolean isSigned) {
        return (root, query, cb) -> {
            if (isSigned == null) {
                return null;
            }
            if (isSigned) {
                return cb.isNotNull(root.get("patientSignedAt"));
            } else {
                return cb.isNull(root.get("patientSignedAt"));
            }
        };
    }

    /**
     * Filter by expiration status.
     * <p>
     * When isExpired is true, returns only submissions where expiresAt < NOW.
     * When isExpired is false, returns only submissions where expiresAt >= NOW OR expiresAt IS NULL.
     * </p>
     *
     * @param isExpired the expiration filter (can be null)
     * @return specification or null if isExpired is null
     */
    private Specification<FormSubmission> isExpired(Boolean isExpired) {
        return (root, query, cb) -> {
            if (isExpired == null) {
                return null;
            }
            LocalDateTime now = LocalDateTime.now();
            if (isExpired) {
                // Expired: expiresAt is not null AND expiresAt < now
                return cb.and(
                        cb.isNotNull(root.get("expiresAt")),
                        cb.lessThan(root.get("expiresAt"), now)
                );
            } else {
                // Not expired: expiresAt is null OR expiresAt >= now
                return cb.or(
                        cb.isNull(root.get("expiresAt")),
                        cb.greaterThanOrEqualTo(root.get("expiresAt"), now)
                );
            }
        };
    }

    /**
     * Filter by user who signed the form (any signature field).
     *
     * @param signedByUserId the user ID to filter by (can be null)
     * @return specification or null if signedByUserId is null
     */
    private Specification<FormSubmission> hasSignedByUser(UUID signedByUserId) {
        return (root, query, cb) -> {
            if (signedByUserId == null) {
                return null;
            }
            // Subquery to check if any signature exists with this user
            var subquery = query.subquery(UUID.class);
            var signatureRoot = subquery.from(com.example.policlicabine.entity.FormSignature.class);
            subquery.select(signatureRoot.get("formSubmission").get("id"))
                    .where(cb.equal(signatureRoot.get("signedBy").get("userId"), signedByUserId));
            
            return cb.in(root.get("id")).value(subquery);
        };
    }
}
