package com.example.policlicabine.repository;

import com.example.policlicabine.entity.FormSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID>,
                                                   JpaSpecificationExecutor<FormSubmission> {

    @EntityGraph(attributePaths = {"template", "patient", "appointmentSession", "attachedFiles", "signatures", "signatures.signedBy"})
    Optional<FormSubmission> findWithDetailsById(UUID id);

    @Query("""
        SELECT fs FROM FormSubmission fs
        WHERE fs.patient.patientId = :patientId
          AND fs.template.id = :templateId
          AND fs.isDeleted = false
          AND (fs.expiresAt IS NULL OR fs.expiresAt > :targetDate)
        ORDER BY fs.submittedAt DESC
        """)
    Optional<FormSubmission> findValidSubmissionForTemplate(
            @Param("patientId") UUID patientId,
            @Param("templateId") UUID templateId,
            @Param("targetDate") LocalDateTime targetDate
    );

    @Query(value = """
        SELECT * FROM form_submissions
        WHERE expires_at BETWEEN :now AND :futureDate
        AND is_deleted = false
        ORDER BY expires_at ASC
        """, nativeQuery = true)
    List<FormSubmission> findExpiringSoon(
            @Param("now") LocalDateTime now,
            @Param("futureDate") LocalDateTime futureDate
    );

    @Query(value = """
        SELECT * FROM form_submissions
        WHERE data @> CAST(:jsonPath AS jsonb)
        AND is_deleted = false
        ORDER BY submitted_at DESC
        """, nativeQuery = true)
    List<FormSubmission> searchInData(@Param("jsonPath") String jsonPath);

    List<FormSubmission> findByPatientPatientIdAndIsDeletedFalse(UUID patientId);

    List<FormSubmission> findByPatientPatientIdAndTemplateIdAndIsDeletedFalse(UUID patientId, UUID templateId);

    List<FormSubmission> findByAppointmentSessionSessionIdAndIsDeletedFalse(UUID sessionId);

    /**
     * Check if patient has a valid submission for a specific template.
     * Used at booking time to verify form requirements.
     */
    @Query("""
        SELECT COUNT(fs) > 0 FROM FormSubmission fs
        WHERE fs.patient.patientId = :patientId
          AND fs.template.id = :templateId
          AND fs.isDeleted = false
          AND (fs.expiresAt IS NULL OR fs.expiresAt > :targetDate)
        """)
    boolean existsValidSubmission(
            @Param("patientId") UUID patientId,
            @Param("templateId") UUID templateId,
            @Param("targetDate") LocalDateTime targetDate);

    // ===== BATCH QUERIES =====

    @EntityGraph(attributePaths = {"template", "patient"})
    List<FormSubmission> findByPatientPatientIdInAndIsDeletedFalse(List<UUID> patientIds);

    // ===== ENTITYGRAPH QUERIES =====

    @EntityGraph(attributePaths = {"template", "patient"})
    List<FormSubmission> findWithTemplateAndPatientByPatientPatientIdAndIsDeletedFalse(UUID patientId);

    @EntityGraph(attributePaths = {"template", "patient", "appointmentSession"})
    List<FormSubmission> findWithAllByAppointmentSessionSessionIdAndIsDeletedFalse(UUID sessionId);

    /**
     * Delete all form submissions for a specific template.
     * Used when hard-deleting a form template.
     */
    @Modifying
    @Transactional
    void deleteByTemplateId(UUID templateId);
}
