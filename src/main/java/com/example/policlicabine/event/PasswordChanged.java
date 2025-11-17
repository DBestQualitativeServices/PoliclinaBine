package com.example.policlicabine.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PasswordChanged(
    UUID userId,
    String username,
    OffsetDateTime changedAt
) {}
