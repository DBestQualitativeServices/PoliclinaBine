package com.example.policlicabine.event;

import com.example.policlicabine.entity.enums.FormPurpose;

import java.util.UUID;

public record FormTemplateCreated(
    UUID templateId,
    String code,
    String name,
    FormPurpose purpose,
    Integer version,
    UUID createdByUserId
) {}
