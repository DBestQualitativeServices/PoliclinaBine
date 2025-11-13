package com.example.policlicabine.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordReset(
    UUID userId,
    String username,
    LocalDateTime resetAt
) {}
