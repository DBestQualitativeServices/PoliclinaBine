package com.example.policlicabine.event;

import java.util.UUID;

public record DoctorProfileCreated(UUID doctorId, UUID userId) {
}
