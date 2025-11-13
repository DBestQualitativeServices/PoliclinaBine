package com.example.policlicabine.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserAuthenticated(
    UUID userId,
    String username,
    LocalDateTime loginTime,
    String ipAddress
) {}
