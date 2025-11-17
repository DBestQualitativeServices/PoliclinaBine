package com.example.policlicabine.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PasswordResetInitiated(
    UUID userId,
    String username,
    String resetToken,
    OffsetDateTime expiryDate
) {}
