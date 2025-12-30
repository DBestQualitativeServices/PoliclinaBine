package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface PatientMapper {

    @Mapping(target = "tutorPatientId", source = "tutor.patientId")
    @Mapping(target = "tutorFirstName", source = "tutor.firstName")
    @Mapping(target = "tutorLastName", source = "tutor.lastName")
    PatientDto toDto(Patient patient);

    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "formSubmissions", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    Patient toEntity(PatientDto dto);
}
