package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    PatientDto toDto(Patient patient);

    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "files", ignore = true)
    @Mapping(target = "forms", ignore = true)
    Patient toEntity(PatientDto dto);
}
