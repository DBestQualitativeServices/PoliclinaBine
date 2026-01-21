package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.FormRequirementStatus;
import com.example.policlicabine.entity.enums.OwnerType;
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
public class FormRequirementDto {
    private UUID templateId;
    private String templateName;
    private FormRequirementStatus status;
    private UUID existingSubmissionId;
    private LocalDateTime expiresAt;

    /**
     * The owner type of this form template.
     * Indicates who must sign for the form to be considered complete.
     */
    private OwnerType ownerType;

    /**
     * List of signature field names that the owner still needs to sign.
     * Only populated when status is PENDING_SIGNATURE.
     */
    private List<String> missingOwnerSignatureFields;
}
