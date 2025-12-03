package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.FormRequirementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormRequirementDto {
    private UUID templateId;
    private String templateName;
    private FormRequirementStatus status;
    private UUID existingSubmissionId;
    private LocalDateTime expiresAt;
}
