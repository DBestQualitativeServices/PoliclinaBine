package com.example.policlicabine.repository;

import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.enums.Specialty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationRepository extends JpaRepository<ConsultationType, UUID> {

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
}
