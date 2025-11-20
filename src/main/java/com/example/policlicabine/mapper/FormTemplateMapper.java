package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.entity.FormTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FormTemplateMapper {

    @Mapping(target = "createdByUserId", source = "createdBy.userId")
    FormTemplateDto toDto(FormTemplate entity);

    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    FormTemplate toEntity(FormTemplateDto dto);
}
