package com.example.policlicabine.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PasswordReset(
    UUID userId,
    String username,
    OffsetDateTime resetAt
) {}
