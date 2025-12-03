package com.example.policlicabine.event;

import java.util.UUID;

public record FormTemplateCreated(
    UUID templateId,
    String name,
    UUID createdByUserId
) {}
