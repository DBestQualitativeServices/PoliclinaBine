package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.ManagerDto;
import com.example.policlicabine.entity.Manager;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ManagerMapper {

    ManagerDto toDto(Manager manager);

    Manager toEntity(ManagerDto dto);
}
