package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.FormSignatureDto;
import com.example.policlicabine.dto.FormSubmissionDto;
import com.example.policlicabine.entity.FormSubmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class FormSubmissionMapper {

    @Autowired
    protected FormSignatureMapper formSignatureMapper;

    @Mapping(target = "templateId", source = "template.id")
    @Mapping(target = "templateName", source = "template.name")
    @Mapping(target = "patientId", source = "patient.patientId")
    @Mapping(target = "patientName", expression = "java(getPatientFullName(entity))")
    @Mapping(target = "appointmentSessionId", source = "appointmentSession.sessionId")
    @Mapping(target = "consultationTypeId", source = "consultationType.consultationId")
    @Mapping(target = "attachedFileIds", expression = "java(getFileIds(entity))")
    @Mapping(target = "signatures", expression = "java(getSignatures(entity))")
    @Mapping(target = "submittedByUserId", source = "submittedBy.userId")
    @Mapping(target = "isExpired", expression = "java(entity.isExpired())")
    @Mapping(target = "isValid", expression = "java(entity.isValid())")
    @Mapping(target = "ownerType", source = "template.ownerType")
    @Mapping(target = "isOwnerSigned", expression = "java(entity.isOwnerSigned())")
    @Mapping(target = "missingOwnerSignatureFields", expression = "java(entity.getMissingOwnerSignatureFields())")
    public abstract FormSubmissionDto toDto(FormSubmission entity);

    @Mapping(target = "template", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "appointmentSession", ignore = true)
    @Mapping(target = "consultationType", ignore = true)
    @Mapping(target = "attachedFiles", ignore = true)
    @Mapping(target = "signatures", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    public abstract FormSubmission toEntity(FormSubmissionDto dto);

    protected String getPatientFullName(FormSubmission entity) {
        if (entity.getPatient() == null) return null;
        return entity.getPatient().getFirstName() + " " + entity.getPatient().getLastName();
    }

    protected List<UUID> getFileIds(FormSubmission entity) {
        if (entity.getAttachedFiles() == null) return List.of();
        return entity.getAttachedFiles().stream()
                .map(file -> file.getId())
                .collect(Collectors.toList());
    }

    protected List<FormSignatureDto> getSignatures(FormSubmission entity) {
        if (entity.getSignatures() == null) return List.of();
        return entity.getSignatures().stream()
                .map(formSignatureMapper::toDto)
                .collect(Collectors.toList());
    }
}
