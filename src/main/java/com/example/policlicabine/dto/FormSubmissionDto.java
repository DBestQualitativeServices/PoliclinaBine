package com.example.policlicabine.dto;

import com.example.policlicabine.model.FormStructure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmissionDto {
    private UUID id;
    private UUID templateId;
    private String templateName;
    private UUID patientId;
    private String patientName;
    private UUID appointmentSessionId;
    private UUID consultationTypeId;
    private FormStructure templateSnapshot;
    private Map<String, Object> data;
    private List<UUID> attachedFileIds;
    private List<FormSignatureDto> signatures;
    private UUID submittedByUserId;
    private LocalDateTime submittedAt;
    private LocalDateTime expiresAt;
    private Boolean isExpired;
    private Boolean isValid;
}
