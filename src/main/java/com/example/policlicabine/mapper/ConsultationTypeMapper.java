package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.ConsultationTypeDto;
import com.example.policlicabine.entity.ConsultationType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConsultationTypeMapper {

    ConsultationTypeDto toDto(ConsultationType consultationType);
}
