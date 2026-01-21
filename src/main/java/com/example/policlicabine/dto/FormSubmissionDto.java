package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.OwnerType;
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

    /**
     * The owner type of this form (from template).
     * Indicates who must sign for the form to be considered complete.
     */
    private OwnerType ownerType;

    /**
     * Whether the owner has signed this form.
     * True if a signature exists for a signature field matching the ownerType.
     */
    private Boolean isOwnerSigned;

    /**
     * List of signature field names that the owner still needs to sign.
     * Empty if isOwnerSigned is true.
     */
    private List<String> missingOwnerSignatureFields;
}
