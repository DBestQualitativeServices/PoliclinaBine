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
public class QuestionDto {

    private UUID questionId;
    private String questionText;
    private UUID consultationId;
    private String consultationName;
    private OffsetDateTime createdAt;
}
