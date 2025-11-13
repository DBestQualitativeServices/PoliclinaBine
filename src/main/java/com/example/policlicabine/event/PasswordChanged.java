package com.example.policlicabine.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordChanged(
    UUID userId,
    String username,
    LocalDateTime changedAt
) {}
