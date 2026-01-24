package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.DiagnosisDto;
import com.example.policlicabine.entity.Diagnosis;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DiagnosisMapper {

    DiagnosisDto toDto(Diagnosis diagnosis);

    Diagnosis toEntity(DiagnosisDto dto);
}
