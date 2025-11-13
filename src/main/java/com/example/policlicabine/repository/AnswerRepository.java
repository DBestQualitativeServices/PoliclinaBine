package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Answer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findBySessionSessionId(UUID sessionId);

    List<Answer> findByQuestionQuestionId(UUID questionId);

    List<Answer> findBySessionSessionIdAndConsultationConsultationId(UUID sessionId, UUID consultationId);

    // EntityGraph methods to prevent N+1 queries when mapping to DTO
    @EntityGraph(attributePaths = {"session", "question", "consultation"})
    Optional<Answer> findWithRelationshipsByAnswerId(UUID answerId);

    @EntityGraph(attributePaths = {"session", "question", "consultation"})
    List<Answer> findWithRelationshipsBySessionSessionId(UUID sessionId);

    @EntityGraph(attributePaths = {"session", "question", "consultation"})
    List<Answer> findWithRelationshipsBySessionSessionIdAndConsultationConsultationId(UUID sessionId, UUID consultationId);
}
