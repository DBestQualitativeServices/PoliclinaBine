package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.PatientDto;
import com.example.policlicabine.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface PatientMapper {

    PatientDto toDto(Patient patient);

    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "formSubmissions", ignore = true)
    @Mapping(target = "user", ignore = true)
    Patient toEntity(PatientDto dto);
}
