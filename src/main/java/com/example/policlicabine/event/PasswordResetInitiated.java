package com.example.policlicabine.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordResetInitiated(
    UUID userId,
    String username,
    String resetToken,
    LocalDateTime expiryDate
) {}
