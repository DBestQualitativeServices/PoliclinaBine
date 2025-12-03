package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormReadinessDto {
    private UUID patientId;
    private UUID appointmentSessionId;
    private LocalDateTime appointmentDate;
    private int totalRequired;
    private int validCount;
    private int missingCount;
    private int expiredCount;
    private int pendingSignatureCount;
    private boolean allFormsComplete;
    private List<FormRequirementDto> requirements;
}
