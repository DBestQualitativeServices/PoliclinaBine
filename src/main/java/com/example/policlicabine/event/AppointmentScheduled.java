package com.example.policlicabine.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentScheduled(
    UUID sessionId,
    UUID patientId,
    UUID doctorId,
    OffsetDateTime scheduledDateTime,
    List<String> consultationNames,
    boolean isEmergency
) {}
