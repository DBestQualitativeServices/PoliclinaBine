package com.example.policlicabine.builder;

import com.example.policlicabine.entity.Consultation;
import com.example.policlicabine.entity.Question;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Test data builder for Question entity using the Builder pattern.
 * <p>
 * Provides common medical history questions for testing.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * Consultation consultation = ConsultationTestBuilder.generalConsultation().build();
 * Question question = QuestionTestBuilder.aQuestion()
 *     .withConsultation(consultation)
 *     .withQuestionText("Do you have any allergies?")
 *     .build();
 * </pre>
 */
public class QuestionTestBuilder {

    private UUID questionId = UUID.randomUUID();
    private Consultation consultation = ConsultationTestBuilder.generalConsultation().build();
    private String questionText = "Do you have any previous medical conditions?";

    public static QuestionTestBuilder aQuestion() {
        return new QuestionTestBuilder();
    }

    public static QuestionTestBuilder allergiesQuestion() {
        return new QuestionTestBuilder()
                .withQuestionText("Do you have any allergies? Please specify.");
    }

    public static QuestionTestBuilder medicationsQuestion() {
        return new QuestionTestBuilder()
                .withQuestionText("Are you currently taking any medications?");
    }

    public static QuestionTestBuilder smokingQuestion() {
        return new QuestionTestBuilder()
                .withQuestionText("Do you smoke?");
    }

    public static QuestionTestBuilder painLevelQuestion() {
        return new QuestionTestBuilder()
                .withQuestionText("On a scale of 1-10, how would you rate your pain level?");
    }

    public QuestionTestBuilder withQuestionId(UUID questionId) {
        this.questionId = questionId;
        return this;
    }

    public QuestionTestBuilder withConsultation(Consultation consultation) {
        this.consultation = consultation;
        return this;
    }

    public QuestionTestBuilder withQuestionText(String questionText) {
        this.questionText = questionText;
        return this;
    }

    public Question build() {
        return Question.builder()
                .questionId(questionId)
                .consultation(consultation)
                .questionText(questionText)
                .answers(new ArrayList<>())
                .build();
    }
}
