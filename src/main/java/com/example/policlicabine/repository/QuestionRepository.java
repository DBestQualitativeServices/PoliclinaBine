package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByConsultationConsultationId(UUID consultationId);

    @EntityGraph(attributePaths = {"consultation"})
    Optional<Question> findWithConsultationByQuestionId(UUID questionId);
}
