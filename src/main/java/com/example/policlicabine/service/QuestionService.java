package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.QuestionDto;
import com.example.policlicabine.entity.Consultation;
import com.example.policlicabine.entity.Question;
import com.example.policlicabine.event.QuestionCreated;
import com.example.policlicabine.event.QuestionDeleted;
import com.example.policlicabine.event.QuestionUpdated;
import com.example.policlicabine.mapper.QuestionMapper;
import com.example.policlicabine.repository.QuestionRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Question entities.
 * Questions are associated with Consultations and used to gather patient information.
 *
 * Architecture:
 * - Extends BaseServiceImpl for common CRUD operations (findById, validateExists, getEntityById)
 * - Only uses QuestionRepository (single responsibility)
 * - Calls ConsultationService for consultation validation and access
 * - Uses EntityGraph to prevent N+1 queries
 * - Follows service-to-service communication pattern
 *
 * Inherited Methods (from BaseServiceImpl):
 * - findById(UUID) → Result&lt;QuestionDto&gt;
 * - validateExists(UUID) → Result&lt;Void&gt;
 * - getEntityById(UUID) → Question
 * - getEntitiesByIds(List&lt;UUID&gt;) → List&lt;Question&gt;
 * - findAll() → Result&lt;List&lt;QuestionDto&gt;&gt;
 */
@Service
@Slf4j
@Transactional
public class QuestionService extends BaseServiceImpl<Question, QuestionDto, UUID> {

    // Only our repository - single responsibility principle
    private final QuestionRepository questionRepository;

    // Service for consultation validation and entity access
    private final ConsultationService consultationService;

    private final QuestionMapper questionMapper;
    private final ApplicationEventPublisher eventPublisher;

    public QuestionService(QuestionRepository questionRepository,
                          ConsultationService consultationService,
                          QuestionMapper questionMapper,
                          ApplicationEventPublisher eventPublisher) {
        super(questionRepository, questionMapper);
        this.questionRepository = questionRepository;
        this.consultationService = consultationService;
        this.questionMapper = questionMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected QuestionDto toDto(Question entity) {
        return questionMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Question";
    }

    @Override
    protected void updateEntityFromDto(Question entity, QuestionDto dto) {
        // Update mutable fields (NOT questionId, consultationId, or createdAt)
        if (dto.getQuestionText() != null && !dto.getQuestionText().trim().isEmpty()) {
            entity.setQuestionText(dto.getQuestionText().trim());
        }
        // Don't update consultation (immutable relationship)
    }

    /**
     * Creates a new question for a consultation.
     * Manages bidirectional relationship with Consultation.
     *
     * Architecture notes:
     * - Uses ConsultationService.getEntityByName() to get consultation entity
     * - Validates consultation exists via service
     *
     * @param consultationName Consultation name
     * @param questionText The question text
     * @return Result containing QuestionDto or error message
     */
    public Result<QuestionDto> createQuestion(String consultationName, String questionText) {
        try {
            if (consultationName == null || consultationName.trim().isEmpty()) {
                return Result.failure("Consultation name is required");
            }

            if (questionText == null || questionText.trim().isEmpty()) {
                return Result.failure("Question text is required");
            }

            // Get consultation entity via ConsultationService
            Consultation consultation = consultationService.getEntityByName(consultationName.trim());
            if (consultation == null) {
                return Result.failure("Consultation not found or inactive");
            }

            Question question = Question.builder()
                .consultation(consultation)
                .questionText(questionText.trim())
                .build();

            // Use bidirectional helper method for proper relationship management
            consultation.addQuestion(question);

            Question savedQuestion = questionRepository.save(question);

            // Publish domain event
            eventPublisher.publishEvent(new QuestionCreated(
                savedQuestion.getQuestionId(),
                consultation.getConsultationId(),
                consultationName,
                savedQuestion.getQuestionText()
            ));

            log.info("Question created: {} for consultation {}", savedQuestion.getQuestionId(), consultationName);

            return Result.success(questionMapper.toDto(savedQuestion));

        } catch (Exception e) {
            log.error("Error creating question", e);
            return Result.failure("Failed to create question: " + e.getMessage());
        }
    }

    /**
     * Retrieves all questions for a specific consultation.
     * Uses read-only transaction for optimal performance.
     *
     * @param consultationId Consultation identifier
     * @return Result containing list of QuestionDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<QuestionDto>> getQuestionsForConsultation(UUID consultationId) {
        try {
            if (consultationId == null) {
                return Result.failure("Consultation ID is required");
            }

            List<Question> questions = questionRepository
                .findByConsultationConsultationId(consultationId);

            List<QuestionDto> questionDtos = questions.stream()
                .map(questionMapper::toDto)
                .collect(Collectors.toList());

            return Result.success(questionDtos);

        } catch (Exception e) {
            log.error("Error getting questions for consultation", e);
            return Result.failure("Failed to get questions: " + e.getMessage());
        }
    }

    /**
     * Finds a question by its unique identifier.
     * Delegates to inherited findById() method from BaseServiceImpl.
     *
     * @param questionId Question identifier
     * @return Result containing QuestionDto or error message
     */
    @Transactional(readOnly = true)
    public Result<QuestionDto> findQuestionById(UUID questionId) {
        return findById(questionId);
    }

    /**
     * Updates an existing question's text.
     *
     * @param questionId Question identifier
     * @param newQuestionText New question text
     * @return Result containing updated QuestionDto or error message
     */
    public Result<QuestionDto> updateQuestionText(UUID questionId, String newQuestionText) {
        try {
            if (questionId == null) {
                return Result.failure("Question ID is required");
            }

            if (newQuestionText == null || newQuestionText.trim().isEmpty()) {
                return Result.failure("Question text is required");
            }

            Question question = questionRepository.findById(questionId)
                .orElse(null);
            if (question == null) {
                return Result.failure("Question not found");
            }

            // Store old text for event
            String oldQuestionText = question.getQuestionText();

            question.setQuestionText(newQuestionText.trim());
            Question savedQuestion = questionRepository.save(question);

            // Publish domain event
            eventPublisher.publishEvent(new QuestionUpdated(
                questionId,
                oldQuestionText,
                newQuestionText.trim()
            ));

            log.info("Question updated: {}", questionId);

            return Result.success(questionMapper.toDto(savedQuestion));

        } catch (Exception e) {
            log.error("Error updating question", e);
            return Result.failure("Failed to update question: " + e.getMessage());
        }
    }

    /**
     * Deletes a question.
     * Note: This will use orphanRemoval from Consultation entity.
     *
     * @param questionId Question identifier
     * @return Result containing success flag or error message
     */
    public Result<Void> deleteQuestion(UUID questionId) {
        try {
            if (questionId == null) {
                return Result.failure("Question ID is required");
            }

            Question question = questionRepository.findById(questionId)
                .orElse(null);
            if (question == null) {
                return Result.failure("Question not found");
            }

            // Store data for event before deletion
            Consultation consultation = question.getConsultation();
            UUID consultationId = consultation != null ? consultation.getConsultationId() : null;
            String questionText = question.getQuestionText();

            // Use bidirectional helper for clean deletion
            if (consultation != null) {
                consultation.removeQuestion(question);
            }

            questionRepository.delete(question);

            // Publish domain event
            eventPublisher.publishEvent(new QuestionDeleted(
                questionId,
                consultationId,
                questionText
            ));

            log.info("Question deleted: {}", questionId);

            return Result.success(null);

        } catch (Exception e) {
            log.error("Error deleting question", e);
            return Result.failure("Failed to delete question: " + e.getMessage());
        }
    }

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============
    // Note: The following methods are inherited from BaseServiceImpl:
    // - getEntityById(UUID) → Question
    // - validateExists(UUID) → Result<Void>
    // - getEntitiesByIds(List<UUID>) → List<Question>

    /**
     * INTERNAL: Gets a question entity with consultation loaded.
     * Uses EntityGraph to prevent N+1 queries.
     * Used by other services when they need question with consultation data.
     *
     * @param questionId Question identifier
     * @return Question entity with consultation or null if not found
     */
    @Transactional(readOnly = true)
    public Question getEntityWithConsultation(UUID questionId) {
        if (questionId == null) {
            return null;
        }
        return questionRepository.findWithConsultationByQuestionId(questionId).orElse(null);
    }

    /**
     * INTERNAL: Validates that a question exists.
     * Delegates to inherited validateExists() method from BaseServiceImpl.
     * Used by other services (e.g., AnswerService) to validate question references.
     *
     * @param questionId Question identifier
     * @return Result success if question exists, failure with message otherwise
     */
    @Transactional(readOnly = true)
    public Result<Void> validateQuestionExists(UUID questionId) {
        return validateExists(questionId);
    }
}
