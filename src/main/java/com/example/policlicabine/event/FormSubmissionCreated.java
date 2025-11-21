package com.example.policlicabine.event;

import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.entity.enums.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FormSubmissionCreated(
    UUID submissionId,
    UUID templateId,
    String templateName,
    UUID patientId,
    UUID appointmentSessionId,
    UUID consultationTypeId,
    SubmissionStatus status,
    LocalDateTime submittedAt,
    LocalDateTime expiresAt,
    UUID submittedByUserId
) {}
