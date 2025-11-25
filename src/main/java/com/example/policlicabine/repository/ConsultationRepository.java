package com.example.policlicabine.repository;

import com.example.policlicabine.common.repository.FilterableRepository;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.enums.Specialty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationRepository extends FilterableRepository<ConsultationType, UUID> {

    Optional<ConsultationType> findByNameAndIsActiveTrue(String name);

    List<ConsultationType> findByNameInAndIsActiveTrue(List<String> names);

    List<ConsultationType> findBySpecialtyInAndIsActiveTrue(List<Specialty> specialties);

    List<ConsultationType> findByIsActiveTrue();

    List<ConsultationType> findBySpecialty(Specialty specialty);

    // ============= EntityGraph Query Methods =============

    /**
     * Finds consultation with questions loaded.
     * Use for DTO mapping with nested QuestionDto list.
     */
    @EntityGraph(attributePaths = {"questions"})
    Optional<ConsultationType> findWithQuestionsByConsultationId(UUID consultationId);

    /**
     * Finds consultations by names with questions loaded.
     * Use when mapping multiple consultations to DTOs.
     */
    @EntityGraph(attributePaths = {"questions"})
    List<ConsultationType> findWithQuestionsByNameInAndIsActiveTrue(List<String> names);

    // ============= Form Template EntityGraph Methods =============

    /**
     * Finds consultation with required form templates loaded.
     * Use for getting/setting the requiredFormTemplates relationship.
     */
    @EntityGraph(attributePaths = {"requiredFormTemplates"})
    Optional<ConsultationType> findWithRequiredFormTemplatesByConsultationId(UUID id);

    /**
     * Finds consultation with consultation form template loaded.
     * Use for getting/setting the consultationFormTemplate relationship.
     */
    @EntityGraph(attributePaths = {"consultationFormTemplate"})
    Optional<ConsultationType> findWithConsultationFormTemplateByConsultationId(UUID id);

    /**
     * Finds consultation with all form templates loaded.
     * Use when both requiredFormTemplates and consultationFormTemplate are needed.
     */
    @EntityGraph(attributePaths = {"requiredFormTemplates", "consultationFormTemplate"})
    Optional<ConsultationType> findWithAllFormTemplatesByConsultationId(UUID id);
}
