package com.example.policlicabine.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record FormSubmissionCreated(
    UUID submissionId,
    UUID templateId,
    String templateName,
    UUID patientId,
    UUID appointmentSessionId,
    UUID consultationTypeId,
    LocalDateTime submittedAt,
    LocalDateTime expiresAt,
    UUID submittedByUserId
) {}
