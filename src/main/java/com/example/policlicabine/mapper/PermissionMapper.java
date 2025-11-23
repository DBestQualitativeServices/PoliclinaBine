package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.PermissionDto;
import com.example.policlicabine.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionDto toDto(Permission permission);

    @Mapping(target = "roles", ignore = true)
    Permission toEntity(PermissionDto dto);
}
