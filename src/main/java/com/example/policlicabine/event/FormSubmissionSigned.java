package com.example.policlicabine.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record FormSubmissionSigned(
    UUID submissionId,
    UUID patientId,
    UUID witnessedByUserId,
    LocalDateTime signedAt
) {}
