package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Filter criteria for searching form submissions.
 * All fields are optional - null values are ignored in search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmissionFilterCriteria {

    private UUID id;

    private UUID patientId;

    private String patientName;

    private UUID templateId;

    private String templateName;

    private UUID appointmentSessionId;

    private UUID consultationTypeId;

    private LocalDateTime submittedAfter;

    private LocalDateTime submittedBefore;

    private LocalDateTime expiresAfter;

    private LocalDateTime expiresBefore;

    private Boolean isSigned;

    private Boolean isExpired;

    /**
     * Filter by user who signed the form (any signature field).
     */
    private UUID signedByUserId;

    /**
     * Filter by whether form is fully signed (all required signatures present).
     */
    private Boolean isFullySigned;
}
