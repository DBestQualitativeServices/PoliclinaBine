package com.example.policlicabine.event;

import com.example.policlicabine.entity.enums.FormPurpose;

import java.util.UUID;

public record FormTemplatePublished(
    UUID templateId,
    String code,
    Integer version,
    FormPurpose purpose
) {}
