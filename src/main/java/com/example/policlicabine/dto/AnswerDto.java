package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDto {

    private UUID answerId;
    private UUID sessionId;
    private UUID questionId;
    private String questionText;
    private UUID consultationId;
    private String consultationName;
    private String answerText;
    private OffsetDateTime createdAt;
}
