package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.RoleDto;
import com.example.policlicabine.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    RoleDto toDto(Role role);

    @Mapping(target = "users", ignore = true)
    Role toEntity(RoleDto dto);
}
