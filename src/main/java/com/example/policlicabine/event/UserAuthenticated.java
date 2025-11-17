package com.example.policlicabine.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserAuthenticated(
    UUID userId,
    String username,
    OffsetDateTime loginTime,
    String ipAddress
) {}
