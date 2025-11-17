package com.example.policlicabine.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionStarted(
    UUID sessionId,
    UUID patientId,
    UUID doctorId,
    OffsetDateTime startedAt
) {}
