package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.AppointmentSessionDto;
import com.example.policlicabine.entity.AppointmentSession;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {
    PatientMapper.class,
    DoctorMapper.class,
    ConsultationTypeMapper.class,
    DiagnosisMapper.class
})
public interface AppointmentSessionMapper {

    /**
     * Maps AppointmentSession entity to DTO with nested DTOs (going DOWN the hierarchy).
     *
     * MapStruct automatically maps:
     * - patient → PatientDto (via PatientMapper)
     * - doctor → DoctorDto (via DoctorMapper)
     * - consultationTypes → List<ConsultationTypeDto> (via ConsultationTypeMapper)
     * - diagnoses → List<DiagnosisDto> (via DiagnosisMapper)
     */
    AppointmentSessionDto toDto(AppointmentSession session);
}
