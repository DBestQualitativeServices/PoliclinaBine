package com.example.policlicabine.repository;

import com.example.policlicabine.entity.FormSubmission;
import com.example.policlicabine.entity.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID> {

    @EntityGraph(attributePaths = {"template", "patient", "appointmentSession", "attachedFiles"})
    Optional<FormSubmission> findWithDetailsById(UUID id);

    @Query(value = """
        SELECT * FROM form_submissions fs
        JOIN form_templates ft ON fs.template_id = ft.id
        WHERE fs.patient_id = :patientId
        AND ft.purpose = CAST(:purpose AS text)
        AND fs.status = 'SIGNED'
        AND (fs.expires_at IS NULL OR fs.expires_at > :now)
        AND fs.is_deleted = false
        ORDER BY fs.submitted_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<FormSubmission> findValidFormByPatientAndPurpose(
            @Param("patientId") UUID patientId,
            @Param("purpose") String purpose,
            @Param("now") LocalDateTime now
    );

    @Query(value = """
        SELECT * FROM form_submissions
        WHERE expires_at BETWEEN :now AND :futureDate
        AND status = 'SIGNED'
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

    List<FormSubmission> findByAppointmentSessionSessionIdAndIsDeletedFalse(UUID sessionId);

    List<FormSubmission> findByStatusAndIsDeletedFalse(SubmissionStatus status);
}
