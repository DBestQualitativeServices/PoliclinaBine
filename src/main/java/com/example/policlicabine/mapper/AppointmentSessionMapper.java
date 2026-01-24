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

    AppointmentSessionDto toDto(AppointmentSession session);
}
