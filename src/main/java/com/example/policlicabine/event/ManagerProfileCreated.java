package com.example.policlicabine.event;

import java.util.UUID;

public record ManagerProfileCreated(
    UUID managerId,
    UUID userId
) {}
