package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Consultation;
import com.example.policlicabine.entity.enums.Specialty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    Optional<Consultation> findByNameAndIsActiveTrue(String name);

    List<Consultation> findByNameInAndIsActiveTrue(List<String> names);

    List<Consultation> findBySpecialtyInAndIsActiveTrue(List<Specialty> specialties);

    List<Consultation> findByIsActiveTrue();

    List<Consultation> findBySpecialty(Specialty specialty);

    // ============= EntityGraph Query Methods =============

    /**
     * Finds consultation with questions loaded.
     * Use for DTO mapping with nested QuestionDto list.
     */
    @EntityGraph(attributePaths = {"questions"})
    Optional<Consultation> findWithQuestionsByConsultationId(UUID consultationId);

    /**
     * Finds consultations by names with questions loaded.
     * Use when mapping multiple consultations to DTOs.
     */
    @EntityGraph(attributePaths = {"questions"})
    List<Consultation> findWithQuestionsByNameInAndIsActiveTrue(List<String> names);
}
