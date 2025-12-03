package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.FormSubmissionDto;
import com.example.policlicabine.entity.FormSubmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface FormSubmissionMapper {

    @Mapping(target = "templateId", source = "template.id")
    @Mapping(target = "templateName", source = "template.name")
    @Mapping(target = "patientId", source = "patient.patientId")
    @Mapping(target = "patientName", expression = "java(getPatientFullName(entity))")
    @Mapping(target = "appointmentSessionId", source = "appointmentSession.sessionId")
    @Mapping(target = "consultationTypeId", source = "consultationType.consultationId")
    @Mapping(target = "attachedFileIds", expression = "java(getFileIds(entity))")
    @Mapping(target = "submittedByUserId", source = "submittedBy.userId")
    @Mapping(target = "patientSignedByUserId", source = "patientSignedBy.userId")
    @Mapping(target = "doctorSignedByUserId", source = "doctorSignedBy.userId")
    @Mapping(target = "isExpired", expression = "java(entity.isExpired())")
    @Mapping(target = "isValid", expression = "java(entity.isValid())")
    FormSubmissionDto toDto(FormSubmission entity);

    @Mapping(target = "template", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "appointmentSession", ignore = true)
    @Mapping(target = "consultationType", ignore = true)
    @Mapping(target = "attachedFiles", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "patientSignedBy", ignore = true)
    @Mapping(target = "doctorSignedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    FormSubmission toEntity(FormSubmissionDto dto);

    default String getPatientFullName(FormSubmission entity) {
        if (entity.getPatient() == null) return null;
        return entity.getPatient().getFirstName() + " " + entity.getPatient().getLastName();
    }

    default List<UUID> getFileIds(FormSubmission entity) {
        if (entity.getAttachedFiles() == null) return List.of();
        return entity.getAttachedFiles().stream()
                .map(file -> file.getId())
                .collect(Collectors.toList());
    }
}
