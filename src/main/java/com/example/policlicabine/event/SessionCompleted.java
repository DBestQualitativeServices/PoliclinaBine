package com.example.policlicabine.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SessionCompleted(
    UUID sessionId,
    UUID patientId,
    UUID doctorId,
    OffsetDateTime completedAt,
    List<String> consultationNames
) {}
